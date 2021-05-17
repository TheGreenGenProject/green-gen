package org.greengen.impl.inmemory

import cats.effect.IO
import cats.implicits._
import org.greengen.core.challenge._
import org.greengen.core.follower.FollowerService
import org.greengen.core.notification.{ChallengeAcceptedNotification, ChallengeRejectedNotification, Notification, NotificationService, YouHaveBeenChallengedNotification}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core._

import scala.collection.concurrent.TrieMap


@deprecated
class InMemoryChallengeService(clock: Clock,
                               userService: UserService[IO],
                               followerService: FollowerService[IO],
                               notificationService: NotificationService[IO]) extends ChallengeService[IO] {
  val PageSize = 10

  val RejectedChallenge = Left(())

  private[this] val challenges = new TrieMap[ChallengeId, Challenge]
  private[this] val authors    = new TrieMap[UserId, Set[ChallengeId]]
  private[this] val challenged = new TrieMap[ChallengeId, Set[UserId]]
  private[this] val results    = new TrieMap[(UserId, ChallengeId), Either[Unit, List[ChallengeStepReportEntry]]]


  override def create(author: UserId,
                      title: String,
                      content: String,
                      schedule: Schedule,
                      successMeasure: SuccessMeasure): IO[ChallengeId] = for {
    _           <- checkUser(author)
    challengeId <- IO(ChallengeId.newId())
    challenge   <- IO(Challenge(challengeId, author, clock.now(), schedule, ChallengeContent(title, content), successMeasure))
    _           <- IO(indexById(challenge))
    _           <- IO(indexByAuthor(challenge))
  } yield challengeId

  override def byId(challengeId: ChallengeId): IO[Option[Challenge]] = IO {
    challenges.get(challengeId)
  }

  override def byAuthor(author: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] = for {
    asAuthor <- IO(authors.get(author).map(_.toList).getOrElse(List()))
    filtered     <- filterChallengeIds(asAuthor, predicate)
    sorted   <- sortChallengeIds(filtered)
    res      <- IO(PagedResult.page(sorted, page))
  } yield res

  override def byUser(userId: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] = for {
    asAuthor     <- IO(authors.get(userId).map(_.toList).getOrElse(List()))
    asChallengee <- IO(challenged.collect { case (challengeId, challengees) if challengees.contains(userId) => challengeId }.toList)
    indep        <- IO(results.keys.collect { case (contestant, challengeId) if contestant==userId => challengeId }.toList)
    filtered     <- filterChallengeIds((asChallengee ++ indep ++ asAuthor).distinct, predicate)
    sorted       <- sortChallengeIds(filtered)
    res          <- IO(PagedResult.page(sorted, page))
  } yield res

  override def contestants(challengeId: ChallengeId, page: Page): IO[List[UserId]] = for {
    _           <- checkChallenge(challengeId)
    challengees <- IO(challenged.getOrElse(challengeId, Set()).toList)
    res         <- IO(PagedResult.page(challengees, page))
  } yield res

  override def contestantCount(challengeId: ChallengeId): IO[Int] = for {
    _     <- checkChallenge(challengeId)
    count <- IO(challenged.getOrElse(challengeId, Set()).size)
  } yield count

  override def challenge(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    _     <- IO(indexChallengeeByChallenge(userId, challengeId))
    notif <- IO(Notification.from(clock, YouHaveBeenChallengedNotification(challengeId)))
    _     <- notificationService.dispatch(notif, List(userId))
  } yield ()

  override def challengeFollowers(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    followers <- followerService.followers(userId)
    _         <- followers.toList.map(challenge(_, challengeId)).sequence
  } yield ()

  override def statistics(challengeId: ChallengeId): IO[ChallengeStatistics] = for {
    summaries     <- IO(results.collect { case ((_, id), Right(value)) if id == challengeId =>
      ChallengeReportSummary.summary(value)})
    rejectedCount <- IO(results.foldLeft(0) {
      case (count, ((_, id), Left(_))) if id == challengeId => count + 1
      case (count, _) => count})
    reportDates   <- reportDates(challengeId)
    now           <- IO(clock.now())
    elapsed       <- IO(reportDates.takeWhile(_.value <= now.value).size)
    statistics    <- IO(ChallengeStatistics.statistics(summaries.size, rejectedCount, elapsed, reportDates.size, summaries.toList))
  } yield statistics

  override def cancel(userId: UserId, challengeId: ChallengeId): IO[Unit] = IO {
    results.put((userId, challengeId), RejectedChallenge)
  }

  override def accept(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    challenge <- getChallenge(challengeId)
    _         <- IO(results.putIfAbsent((userId, challengeId), Right(List())))
    notif     <- IO(Notification.from(clock, ChallengeAcceptedNotification(challengeId, userId)))
    _         <- notificationService.dispatch(notif, List(challenge.author))
  } yield ()

  override def reject(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    challenge <- getChallenge(challengeId)
    _         <- IO(results.putIfAbsent((userId, challengeId), RejectedChallenge))
    notif     <- IO(Notification.from(clock, ChallengeRejectedNotification(challengeId, userId)))
    _         <- notificationService.dispatch(notif, List(challenge.author))
  } yield ()

  override def reportStep(userId: UserId,
                          challengeId: ChallengeId,
                          step: Int,
                          success: ChallengeStepReportStatus): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- checkChallenge(challengeId)
    key = (userId, challengeId)
    _ <- IOUtils.check(results.contains(key), s"Challenge $challengeId has not been accepted yet !")
    _ <- IOUtils.check(step > 0, s"Invalid challenge step $step - cannot be reported")
    _ <- IO(results.put(key, results.getOrElse(key, Right(List()))
         .map { steps =>
           val filtered = steps.filter(_.step!=step) // making sure we don't already have a report for the given step
           ChallengeStepReportEntry(step, success) :: filtered
         }))
  } yield ()

  override def reportedSteps(userId: UserId, challengeId: ChallengeId): IO[List[ChallengeStepReportEntry]] = IO {
    results.get((userId, challengeId)).map(_.toOption).flatten.getOrElse(List())
  }

  override def hasReportDue(userId: UserId, challengeId: ChallengeId): IO[Boolean] = for {
    accepted <- hasAccepted(userId, challengeId)
    onTracks <- status(userId, challengeId).map(_==OnTracks)
    missing <- IO(hasMissingReports(clock, userId, challengeId))
  } yield accepted && onTracks && missing

  override def hasAccepted(userId: UserId, challengeId: ChallengeId): IO[Boolean] = for {
    _        <- checkUser(userId)
    key = (userId, challengeId)
    accepted <- IO(results.getOrElse(key, RejectedChallenge).isRight)
  } yield accepted

  override def hasRejected(userId: UserId, challengeId: ChallengeId): IO[Boolean] = for {
    _        <- checkUser(userId)
    key = (userId, challengeId)
    rejected <- IO(results.getOrElse(key, Right(List())).isLeft)
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
    key = (userId, challengeId)
    accepted        <- hasAccepted(userId, challengeId)
    rejected        <- hasRejected(userId, challengeId)
    results         <- IO(results.getOrElse(key, Right(List())).getOrElse(List()))
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


  // Indexers

  private[this] def indexById(challenge: Challenge): Unit =
    challenges.put(challenge.id, challenge)

  private[this] def indexByAuthor(challenge: Challenge): Unit =
    authors.updateWith(challenge.author) {
      case Some(ids) => Some(ids + challenge.id)
      case None      => Some(Set(challenge.id))
    }

  private[this] def indexChallengeeByChallenge(challengeeId: UserId, challengeId: ChallengeId): Unit =
    challenged.updateWith(challengeId) {
      case Some(challengees) => Some(challengees + challengeeId)
      case None              => Some(Set(challengeeId))
    }

  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _       <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkChallenge(challengeId: ChallengeId) = for {
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


  // Sorting

  private[this] def sortChallengeIds(challengeIds: List[ChallengeId]): IO[List[ChallengeId]] = for {
    challenges <- challengeIds.map(byId).sequence
    flattened  <- IO(challenges.flatten)
    sorted     <- sortChallenges(flattened)
  } yield sorted.map(_.id)

  private[this] def sortChallenges(challenges: List[Challenge]): IO[List[Challenge]] = IO {
    challenges.sortBy(-1L * _.created.value)
  }

  // Filtering

  private[this] def filterChallengeIds(challengeIds: List[ChallengeId],
                                       p: Challenge => Boolean): IO[List[ChallengeId]] = for {
    challenges <- challengeIds.map(byId).sequence
    flattened  <- IO(challenges.flatten)
    filtered   <- IO(flattened.filter(p))
  } yield filtered.map(_.id)

  private[this] def hasMissingReports(clock: Clock,
                                      userId: UserId,
                                      challengeId: ChallengeId): Boolean = (for {
    reportDates <- reportDates(challengeId)
    now = clock.now().value
    due         <- IO(reportDates.filter(_.value <= now))
    reported    <- reportedSteps(userId, challengeId)
  } yield (due.size - reported.size) > 0).unsafeRunSync()

}
