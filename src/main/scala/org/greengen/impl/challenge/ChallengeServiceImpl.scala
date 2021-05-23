package org.greengen.impl.challenge

import cats.effect.IO
import cats.implicits._
import org.greengen.core._
import org.greengen.core.challenge._
import org.greengen.core.follower.FollowerService
import org.greengen.core.notification._
import org.greengen.core.user.{UserId, UserService}
import org.greengen.store.challenge.ChallengeStore


class ChallengeServiceImpl(challengeStore: ChallengeStore[IO])
                          (clock: Clock,
                           userService: UserService[IO],
                           followerService: FollowerService[IO],
                           notificationService: NotificationService[IO]) extends ChallengeService[IO] {
  val PageSize = 10

  override def create(author: UserId,
                      title: String,
                      content: String,
                      schedule: Schedule,
                      successMeasure: SuccessMeasure): IO[ChallengeId] = for {
    _           <- checkUser(author)
    challengeId <- IO(ChallengeId.newId())
    challenge   <- IO(Challenge(challengeId, author, clock.now(), schedule, ChallengeContent(title, content), successMeasure))
    _           <- challengeStore.register(challenge)
  } yield challengeId

  override def byId(challengeId: ChallengeId): IO[Option[Challenge]] =
    challengeStore.getById(challengeId)

  override def byAuthor(author: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] =
    challengeStore.getByAuthor(author, page, predicate)

  override def byUser(userId: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] =
    challengeStore.getByUser(userId, page, predicate)

  override def contestants(challengeId: ChallengeId, page: Page): IO[List[UserId]] = for {
    _  <- checkChallenge(challengeId)
    res <- challengeStore.getChallengees(challengeId, page)
  } yield res

  override def contestantCount(challengeId: ChallengeId): IO[Long] = for {
    _     <- checkChallenge(challengeId)
    count <- challengeStore.getChallengeeCount(challengeId)
  } yield count

  override def challenge(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    _     <- challengeStore.markUserAsChallenged(userId, challengeId)
    notif <- IO(Notification.from(clock, YouHaveBeenChallengedNotification(challengeId)))
    _     <- notificationService.dispatch(notif, List(userId))
  } yield ()

  override def challengeFollowers(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    followers <- followerService.followers(userId)
    _         <- followers.toList.map(challenge(_, challengeId)).sequence
  } yield ()

  override def statistics(challengeId: ChallengeId): IO[ChallengeStatistics] = for {
    reported      <- challengeStore.getResultsByChallenge(challengeId)
    summaries     <- IO(reported.collect { case Right(value) => ChallengeReportSummary.summary(value)})
    rejectedCount <- IO(reported.foldLeft(0) {
      case (count, Left(_)) => count + 1
      case (count, _)       => count})
    reportDates   <- reportDates(challengeId)
    now           <- IO(clock.now())
    elapsed       <- IO(reportDates.takeWhile(_.value <= now.value).size)
    statistics    <- IO(ChallengeStatistics.statistics(summaries.size, rejectedCount, elapsed, reportDates.size, summaries))
  } yield statistics

  override def cancel(userId: UserId, challengeId: ChallengeId): IO[Unit] =
    challengeStore.cancelChallenge(userId, challengeId)

  override def accept(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    challenge <- getChallenge(challengeId)
    _         <- challengeStore.acceptChallenge(userId, challengeId)
    notif     <- IO(Notification.from(clock, ChallengeAcceptedNotification(challengeId, userId)))
    _         <- notificationService.dispatch(notif, List(challenge.author))
  } yield ()

  override def reject(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    challenge <- getChallenge(challengeId)
    _         <- challengeStore.rejectChallenge(userId, challengeId)
    notif     <- IO(Notification.from(clock, ChallengeRejectedNotification(challengeId, userId)))
    _         <- notificationService.dispatch(notif, List(challenge.author))
  } yield ()

  override def reportStep(userId: UserId,
                          challengeId: ChallengeId,
                          step: Int,
                          status: ChallengeStepReportStatus): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- checkChallenge(challengeId)
    rejected <- hasRejected(userId, challengeId)
    _ <- IOUtils.check(!rejected, s"Challenge $challengeId has been rejected !")
    accepted <- hasAccepted(userId, challengeId)
    _ <- IOUtils.check(accepted, s"Challenge $challengeId has not been accepted yet !")
    _ <- IOUtils.check(step > 0, s"Invalid challenge step $step - cannot be reported")
    _ <- challengeStore.reportStep(userId, challengeId, step, status)
  } yield ()

  override def reportedSteps(userId: UserId, challengeId: ChallengeId): IO[List[ChallengeStepReportEntry]] =
    challengeStore.getStepReports(userId, challengeId)

  override def hasReportDue(userId: UserId, challengeId: ChallengeId): IO[Boolean] = for {
    accepted <- hasAccepted(userId, challengeId)
    onTracks <- status(userId, challengeId).map(_==OnTracks)
    missing <- IO(hasMissingReports(clock, userId, challengeId))
  } yield accepted && onTracks && missing

  override def hasAccepted(userId: UserId, challengeId: ChallengeId): IO[Boolean] = for {
    _        <- checkUser(userId)
    accepted <- challengeStore.isChallengeAccepted(challengeId, userId)
  } yield accepted

  override def hasRejected(userId: UserId, challengeId: ChallengeId): IO[Boolean] = for {
    _        <- checkUser(userId)
    rejected <- challengeStore.isChallengeRejected(challengeId, userId)
  } yield rejected

  override def status(challengeId: ChallengeId): IO[ChallengeStatus] = for {
    challenge <- getChallenge(challengeId)
    started   <- IO(challenge.schedule.hasStarted(clock))
    ended     <- IO(challenge.schedule.isOver(clock))
  } yield (started, ended) match {
    case (false, _    ) => NotYetStarted
    case (true , false) => OnGoing
    case (_    , true ) => Closed
  }

  override def status(userId: UserId, challengeId: ChallengeId): IO[ChallengeOutcomeStatus] = for {
    _               <- checkUser(userId)
    challenge       <- getChallenge(challengeId)
    challengeStatus <- status(challengeId)
    accepted        <- hasAccepted(userId, challengeId)
    rejected        <- hasRejected(userId, challengeId)
    results         <- challengeStore.getStepReports(userId, challengeId)
    summary         <- IO(ChallengeReportSummary.summary(results))
    onTracks        <- IO(ChallengeReportSummary.isOnTracks(challenge, summary))
  } yield (accepted, rejected, challengeStatus, onTracks) match {
    case (_    , true , _            , _ )    => Rejected
    case (true , _    , NotYetStarted, _ )    => Accepted
    case (false, false, NotYetStarted, _ )    => NotYetTaken
    case (true , _    , OnGoing      , true)  => OnTracks
    case (true , _    , OnGoing      , false) => Failed
    case (true , _    , Closed       , false) => Completed
    case _                                    => NotTaken
  }

  override def reportDates(challengeId: ChallengeId): IO[List[UTCTimestamp]] = for {
    challenge <- getChallenge(challengeId)
    nextDate  <- IO(challenge.schedule.ticks().toList)
  } yield nextDate

  override def nextReport(challengeId: ChallengeId): IO[Option[UTCTimestamp]] = for {
    challenge <- getChallenge(challengeId)
    nextDate  <- IO(challenge.schedule.next(clock))
  } yield nextDate



  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _       <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkChallenge(challengeId: ChallengeId): IO[Unit] = for {
    challengeOpt <- byId(challengeId)
    _            <- IOUtils.from(challengeOpt, s"Challenge $challengeId doesn't exist")
    author       <- IO(challengeOpt.get.author)
    enabled      <- userService.isEnabled(author)
    _            <- IOUtils.check(enabled, s"Challenge author $author is disabled")
  } yield ()

  private[this] def getChallenge(challengeId: ChallengeId): IO[Challenge] = for {
    maybeChallenge <- byId(challengeId)
    res            <- IOUtils.from(maybeChallenge, s"Challenge $challengeId doesn't exist")
  } yield res

  // Filtering

  private[this] def hasMissingReports(clock: Clock,
                                      userId: UserId,
                                      challengeId: ChallengeId): Boolean = (for {
    reportDates <- reportDates(challengeId)
    now = clock.now().value
    due         <- IO(reportDates.filter(_.value <= now))
    reported    <- reportedSteps(userId, challengeId)
  } yield (due.size - reported.size) > 0).unsafeRunSync()

}
