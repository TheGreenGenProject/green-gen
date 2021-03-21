package org.greengen.http.like

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.like.LikeService
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpLikeService {

  def routes(service: LikeService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "like" / UUIDVar(id) as _ =>
      service.countLikes(PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "like" / "is-liked" / UUIDVar(id) as userId  =>
      service.isLiked(userId, PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "like" / "add" :?
      PostIdQueryParamMatcher(postId) as userId =>
      service.like(userId, postId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "like" / "remove" :?
      PostIdQueryParamMatcher(postId) as userId =>
      service.unlike(userId, postId).flatMap(r => Ok(r.asJson))
  }

}
