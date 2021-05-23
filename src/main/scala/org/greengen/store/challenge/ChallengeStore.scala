package org.greengen.store.challenge

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.challenge.{Challenge, ChallengeId, ChallengeStepReportEntry, ChallengeStepReportStatus}
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait ChallengeStore[F[_]] extends Store[F] {

  def register(challenge: Challenge): F[()]

  def getById(challengeId: ChallengeId): F[Option[Challenge]]

  def getByAuthor(userId: UserId): F[List[ChallengeId]]

  def getByAuthor(author: UserId,
                  page: Page,
                  predicate: Challenge => Boolean): F[List[ChallengeId]]

  def getByUser(userId: UserId): F[List[ChallengeId]]

  def getByUser(author: UserId,
                page: Page,
                predicate: Challenge => Boolean): F[List[ChallengeId]]

  def getResultsByChallenge(challengeId: ChallengeId): IO[List[Either[Unit, List[ChallengeStepReportEntry]]]]

  def getChallengees(challengeId: ChallengeId, page: Page): F[List[UserId]]

  def getChallengeeCount(challengeId: ChallengeId): F[Long]

  def acceptChallenge(userId: UserId, challengeId: ChallengeId): F[Unit]

  def isChallengeAccepted(challengeId: ChallengeId, byUser: UserId): F[Boolean]

  def isChallengeRejected(challengeId: ChallengeId, byUser: UserId): F[Boolean]

  def rejectChallenge(userId: UserId, challengeId: ChallengeId): F[Unit]

  def cancelChallenge(userId: UserId, challengeId: ChallengeId): F[Unit]

  def getStepReports(userId: UserId, challengeId: ChallengeId): F[List[ChallengeStepReportEntry]]

  def reportStep(userId: UserId, challengeId: ChallengeId, step: Int, report: ChallengeStepReportStatus): F[Unit]

  def markUserAsChallenged(userId: UserId, challengeId: ChallengeId): F[Unit]

}
