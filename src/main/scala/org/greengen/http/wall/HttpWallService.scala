package org.greengen.http.wall

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.user.UserId
import org.greengen.core.wall.WallService
import org.greengen.core.{Page, UUID}
import org.greengen.http.HttpQueryParameters._
import org.http4s.AuthedRoutes
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpWallService {

  val PageSize = 10

  def routes(service: WallService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "wall" / UUIDVar(id) / IntVar(page) as _ =>
      service.wall(UserId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "wall" / "add" :?
      PostIdQueryParamMatcher(postId) as userId =>
      service.addToWall(userId, postId).flatMap(r => Ok(r.asJson))
  }

}

