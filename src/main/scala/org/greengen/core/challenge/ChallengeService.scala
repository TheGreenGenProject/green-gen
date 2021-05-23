package org.greengen.core.challenge

import org.greengen.core.{Clock, Page, Schedule, UTCTimestamp}
import org.greengen.core.user.UserId



trait ChallengeService[F[_]] {

  def create(author: UserId,
             title: String,
             content: String,
             schedule: Schedule,
             successMeasure: SuccessMeasure): F[ChallengeId]

  def byId(challengeId: ChallengeId): F[Option[Challenge]]
  
  def byAuthor(author: UserId,
               page: Page,
               predicate: Challenge => Boolean = ChallengeService.AllChallengesFilter): F[List[ChallengeId]]

  def byUser(userId: UserId,
             page: Page,
             predicate: Challenge => Boolean = ChallengeService.AllChallengesFilter): F[List[ChallengeId]]

  def contestants(challengeId: ChallengeId, page: Page): F[List[UserId]]

  def contestantCount(challengeId: ChallengeId): F[Long]


  // Challenge command

  def challenge(userId: UserId, challengeId: ChallengeId): F[Unit]

  def challengeFollowers(userId: UserId, challengeId: ChallengeId): F[Unit]

  def statistics(challengeId: ChallengeId): F[ChallengeStatistics]

  def cancel(userId: UserId, challengeId: ChallengeId): F[Unit]

  def accept(userId: UserId, challengeId: ChallengeId): F[Unit]

  def reject(userId: UserId, challengeId: ChallengeId): F[Unit]

  def reportStep(userId: UserId,
                 challengeId: ChallengeId,
                 step: Int,
                 success: ChallengeStepReportStatus): F[Unit]

  // Statuses

  def hasAccepted(userId: UserId, challengeId: ChallengeId): F[Boolean]

  def hasRejected(userId: UserId, challengeId: ChallengeId): F[Boolean]

  def reportDates(challengeId: ChallengeId): F[List[UTCTimestamp]]

  def nextReport(challengeId: ChallengeId): F[Option[UTCTimestamp]]

  def status(challengeId: ChallengeId): F[ChallengeStatus]

  def status(userId: UserId, challengeId: ChallengeId): F[ChallengeOutcomeStatus]

  def reportedSteps(userId: UserId, challengeId: ChallengeId): F[List[ChallengeStepReportEntry]]

  def hasReportDue(userId: UserId, challengeId: ChallengeId): F[Boolean]

}

object ChallengeService {

  val AllChallengesFilter = (_: Challenge) => true

  val OnGoingFilter = (clock: Clock, c: Challenge) =>
    c.schedule.hasStarted(clock) && !c.schedule.isOver(clock)

  val FinishedFilter = (clock: Clock, c: Challenge) => c.schedule.isOver(clock)

  val FailedFilter = (_: Clock, c: Challenge, status: Challenge => ChallengeOutcomeStatus) => status(c) == Failed

  val ReportDueFilter = (clock: Clock, c: Challenge, hasReportDue: Challenge => Boolean) =>
    c.schedule.hasStarted(clock) && !c.schedule.isOver(clock) && hasReportDue(c)

  val OnTracksFilter = (clock: Clock, c: Challenge, onTracks: Challenge => Boolean) =>
    c.schedule.hasStarted(clock) && !c.schedule.isOver(clock) && onTracks(c)

  val NotStartedYetFilter = (clock: Clock, c: Challenge) =>
    !c.schedule.hasStarted(clock) && !c.schedule.isOver(clock)

}