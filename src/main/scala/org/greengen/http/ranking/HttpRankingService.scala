package org.greengen.http.ranking

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.ranking.RankingService
import org.greengen.core.user.UserId
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._


object HttpRankingService {

  def routes(service: RankingService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "rank" / UUIDVar(id) as _ =>
      service.rank(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "rank" / "breakdown" / UUIDVar(id) as _ =>
      service.score(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
  }

}
