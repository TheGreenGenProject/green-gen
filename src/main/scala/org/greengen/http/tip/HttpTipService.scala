package org.greengen.http.tip

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.tip.{TipId, TipService}
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpTipService {

  def routes(service: TipService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "tip" / "by-id" / UUIDVar(id) as _ =>
      service.byId(TipId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "tip" / "by-author" / UUIDVar(id) as _ =>
      service.byAuthor(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "tip" / "new" :?
      ContentQueryParamMatcher(content) +&
      SourceListQueryParamMatcher(sources) as userId =>
      service.create(userId, content, sources).flatMap(r => Ok(r.asJson))
  }

}
