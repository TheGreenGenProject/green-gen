package org.greengen.http.pin

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.{Page, UUID}
import org.greengen.core.pin.PinService
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpPinService {

  val PageSize = 30

  def routes(service: PinService[IO]): AuthedRoutes[UserId, IO] = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "pin" / "pinned" / IntVar(page) as userId =>
      service.byUser(userId, Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "pin" / "is-pinned" / UUIDVar(postId) as userId =>
      service.isPinned(userId, PostId(UUID.from(postId))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "pin" / "add" :?
      PostIdQueryParamMatcher(postId) as userId =>
      service.pin(userId, postId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "pin" / "remove" :?
      PostIdQueryParamMatcher(postId) as userId =>
      service.unpin(userId, postId).flatMap(r => Ok(r.asJson))
  }

}
