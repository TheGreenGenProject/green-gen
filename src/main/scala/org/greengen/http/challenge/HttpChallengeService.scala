package org.greengen.http.challenge

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.{Page, UUID}
import org.greengen.core.challenge.{ChallengeId, ChallengeService, Failure, Partial, Skipped, Success}
import org.greengen.core.{Clock, _}
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpChallengeService {

  val PageSize = 10

  def routes(clock: Clock, service: ChallengeService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "challenge" / "by-id" / UUIDVar(id) as _ =>
      service.byId(ChallengeId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-author" / UUIDVar(id) as _ =>
      service.byAuthor(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / UUIDVar(id) as _ =>
      service.byUser(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "contestants" / UUIDVar(id) / IntVar(page) as _ =>
      service.contestants(ChallengeId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "contestant-count" / UUIDVar(id) as _ =>
      service.contestantCount(ChallengeId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "is-accepted" / UUIDVar(challengeId) as userId =>
      service.hasAccepted(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "is-rejected" / UUIDVar(challengeId) as userId =>
      service.hasRejected(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "status" / UUIDVar(challengeId) as _ =>
      service.status(ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "status" / UUIDVar(challengeId) / "for-user" / UUIDVar(userId) as _ =>
      service.status(UserId(UUID.from(userId)), ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "report" / "dates" / UUIDVar(challengeId) as _ =>
      service.reportDates(ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "reported" / UUIDVar(challengeId) as userId =>
      service.reportedSteps(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "statistics" / UUIDVar(challengeId) as _ =>
      service.statistics(ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "challenge" / "new" :?
        TitleQueryParamMatcher(title) +&
        ContentQueryParamMatcher(content) +&
        ScheduleQueryParamMatcher(schedule) as userId =>
      service.create(userId, title, content, schedule, ???).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "user" / UUIDVar(userId) / "to" / UUIDVar(challengeId) as _ =>
      service.challenge(UserId(UUID.from(userId)), ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "followers" / "to" / UUIDVar(challengeId) as userId =>
      service.challengeFollowers(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "accept" / UUIDVar(challengeId) as userId =>
      service.accept(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "reject" / UUIDVar(challengeId) as userId =>
      service.reject(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "cancel" / UUIDVar(challengeId) as userId =>
      service.cancel(userId, ChallengeId(UUID.from(challengeId))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "report" / "success" / IntVar(step) / UUIDVar(challengeId) as userId =>
      service.reportStep(userId, ChallengeId(UUID.from(challengeId)), step, Success).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "report" / "partial-success" / IntVar(step) / UUIDVar(challengeId) as userId =>
      service.reportStep(userId, ChallengeId(UUID.from(challengeId)), step, Partial).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "report" / "skipped" / IntVar(step) / UUIDVar(challengeId) as userId =>
      service.reportStep(userId, ChallengeId(UUID.from(challengeId)), step, Skipped).flatMap(r => Ok(r.asJson))
    case POST -> Root / "challenge" / "report" / "failure" / IntVar(step) / UUIDVar(challengeId) as userId =>
      service.reportStep(userId, ChallengeId(UUID.from(challengeId)), step, Failure).flatMap(r => Ok(r.asJson))
  }
}
