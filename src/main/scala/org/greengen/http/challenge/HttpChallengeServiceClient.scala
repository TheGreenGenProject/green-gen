package org.greengen.http.challenge

import cats.effect.IO
import org.greengen.core.{Page, Schedule, UTCTimestamp}
import org.greengen.core.challenge.{Challenge, ChallengeId, ChallengeOutcomeStatus, ChallengeReportSummary, ChallengeService, ChallengeStatistics, ChallengeStatus, ChallengeStepReportEntry, ChallengeStepReportStatus, SuccessMeasure}
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client

class HttpChallengeServiceClient(httpClient: Client[IO], root: Uri) extends ChallengeService[IO] {

  override def create(author: UserId, title: String, content: String, schedule: Schedule, measure: SuccessMeasure): IO[ChallengeId] =
    httpClient.expect[ChallengeId](post(root / "challenge" / "new",
      "user-id" -> author,
      "title" -> title,
      "content" -> content,
      "schedule" -> ???))

  override def byId(challengeId: ChallengeId): IO[Option[Challenge]] =
    httpClient.expect[Option[Challenge]](root / "challenge" /"by-id" / challengeId.value.uuid)

  override def byAuthor(author: UserId): IO[Set[ChallengeId]] =
    httpClient.expect[Set[ChallengeId]](root / "challenge" /"by-author" / author.value.uuid)

  override def byUser(userId: UserId): IO[Set[ChallengeId]] =
    httpClient.expect[Set[ChallengeId]](root / "challenge" /"by-user" / userId.value.uuid)

  override def contestants(challengeId: ChallengeId, page: Page): IO[List[UserId]] = ???

  override def contestantCount(challengeId: ChallengeId): IO[Int] = ???

  override def challenge(userId: UserId, challengeId: ChallengeId): IO[Unit] = ???

  override def challengeFollowers(userId: UserId, challengeId: ChallengeId): IO[Unit] = ???

  override def statistics(challengeId: ChallengeId): IO[ChallengeStatistics] = ???

  override def cancel(userId: UserId, challengeId: ChallengeId): IO[Unit] = ???

  override def accept(userId: UserId, challengeId: ChallengeId): IO[Unit] = ???

  override def reject(userId: UserId, challengeId: ChallengeId): IO[Unit] = ???

  override def reportStep(userId: UserId, challengeId: ChallengeId, step: Int, success: ChallengeStepReportStatus): IO[Unit] = ???

  override def reportedSteps(userId: UserId, challengeId: ChallengeId): IO[List[ChallengeStepReportEntry]] = ???

  override def hasAccepted(userId: UserId, challengeId: ChallengeId): IO[Boolean] = ???

  override def hasRejected(userId: UserId, challengeId: ChallengeId): IO[Boolean] = ???

  override def reportDates(challengeId: ChallengeId): IO[List[UTCTimestamp]] = ???

  override def nextReport(challengeId: ChallengeId): IO[Option[UTCTimestamp]] = ???

  override def status(challengeId: ChallengeId): IO[ChallengeStatus] = ???

  override def status(userId: UserId, challengeId: ChallengeId): IO[ChallengeOutcomeStatus] = ???
}
