package org.greengen.core.challenge

import org.greengen.core.{Page, Schedule, UTCTimestamp}
import org.greengen.core.user.UserId



trait ChallengeService[F[_]] {

  def create(author: UserId,
             title: String,
             content: String,
             schedule: Schedule,
             successMeasure: SuccessMeasure): F[ChallengeId]

  def byId(challengeId: ChallengeId): F[Option[Challenge]]

  def byAuthor(author: UserId): F[Set[ChallengeId]]

  def byUser(userId: UserId): F[Set[ChallengeId]]

  def contestants(challengeId: ChallengeId, page: Page): F[List[UserId]]

  def contestantCount(challengeId: ChallengeId): F[Int]


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

}