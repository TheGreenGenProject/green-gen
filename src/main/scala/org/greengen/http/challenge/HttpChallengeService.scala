package org.greengen.http.challenge

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.challenge.{Challenge, ChallengeId, ChallengeService, Failure, OnTracks, Partial, Skipped, Success}
import org.greengen.core.challenge.ChallengeService._
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Page, UUID}
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
    case GET -> Root / "challenge" / "by-author" / UUIDVar(id) / IntVar(page) as _ =>
      service.byAuthor(UserId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / UUIDVar(id) / IntVar(page) as _ =>
      service.byUser(UserId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / "on-going" / UUIDVar(id) / IntVar(page) as _ =>
      service.byUser(UserId(UUID.from(id)), Page(page, PageSize), OnGoingFilter(clock,_))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / "upcoming" / UUIDVar(id) / IntVar(page) as _ =>
      service.byUser(UserId(UUID.from(id)), Page(page, PageSize), NotStartedYetFilter(clock,_))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / "failed" / UUIDVar(id) / IntVar(page) as _ =>
      val challengeOutcomeFilter = (c: Challenge) =>
        service.status(UserId(UUID.from(id)), c.id).unsafeRunSync()
      service.byUser(UserId(UUID.from(id)),
                     Page(page, PageSize),
                     FailedFilter(clock,_, challengeOutcomeFilter))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / "on-tracks" / UUIDVar(id) / IntVar(page) as _ =>
      val onTracksFilter = (c: Challenge) =>
        service.status(UserId(UUID.from(id)), c.id).map(_==OnTracks).unsafeRunSync()
      service.byUser(UserId(UUID.from(id)), Page(page, PageSize), OnTracksFilter(clock,_, onTracksFilter))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / "report-due" / UUIDVar(id) / IntVar(page) as _ =>
      val hasReportDueFilter = (c: Challenge) =>
        service.hasReportDue(UserId(UUID.from(id)), c.id).unsafeRunSync()
      service.byUser(UserId(UUID.from(id)),
        Page(page, PageSize),
        ReportDueFilter(clock,_, hasReportDueFilter))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "challenge" / "by-user" / "finished" / UUIDVar(id) / IntVar(page) as _ =>
      service.byUser(UserId(UUID.from(id)), Page(page, PageSize), FinishedFilter(clock,_))
        .flatMap(r => Ok(r.asJson))
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
        ScheduleQueryParamMatcher(schedule) +&
        SuccessMeasureQueryParamMatcher(success) as userId =>
      service.create(userId, title, content, schedule, success).flatMap(r => Ok(r.asJson))
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
