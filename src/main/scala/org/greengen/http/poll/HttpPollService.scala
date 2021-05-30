package org.greengen.http.poll

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.challenge.{Challenge => _}
import org.greengen.core.poll.{PollId, PollService}
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Page, UUID}
import org.greengen.http.HttpQueryParameters._
import org.http4s.AuthedRoutes
import org.http4s.circe._
import org.http4s.dsl.io._


object HttpPollService {

  val PageSize = 10

  def routes(clock: Clock, service: PollService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "poll" / "by-id" / UUIDVar(id) as _ =>
      service.byId(PollId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "poll" / "by-author" / UUIDVar(id) / IntVar(page) as _ =>
      service.byAuthor(UserId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "poll" / "is-closed" / UUIDVar(pollId) as _ =>
      service.isClosed(PollId(UUID.from(pollId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "poll" / "has-answered" / UUIDVar(pollId) as userId =>
      service.hasResponded(PollId(UUID.from(pollId)), userId).flatMap(r => Ok(r.asJson))
    case GET -> Root / "poll" / "get-answer" / UUIDVar(pollId) as userId =>
      service.getAnswerFor(PollId(UUID.from(pollId)), userId).flatMap(r => Ok(r.asJson))
    case GET -> Root / "poll" / "repondents" / UUIDVar(pollId) as _ =>
      service.respondents(PollId(UUID.from(pollId))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "poll" / "statistics" / UUIDVar(pollId) as _ =>
      service.statisics(PollId(UUID.from(pollId))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "poll" / "new" :?
      QuestionQueryParamMatcher(question) +&
      PollOptionParamMatcher(options) as userId =>
      service.create(userId, question, options).flatMap(r => Ok(r.asJson))
    case POST -> Root / "poll" / "close" / UUIDVar(pollId) as userId =>
      service.closePoll(userId, PollId(UUID.from(pollId))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "poll" / "answer" / UUIDVar(pollId) / IntVar(optionIndex) as userId =>
      service.answer(PollId(UUID.from(pollId)), userId, optionIndex).flatMap(r => Ok(r.asJson))
  }
}
