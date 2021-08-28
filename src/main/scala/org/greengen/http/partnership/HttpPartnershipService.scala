package org.greengen.http.partnership

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.partnership.{PartnerId, PartnershipService}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.http4s.AuthedRoutes
import org.http4s.circe._
import org.http4s.dsl.impl.UUIDVar
import org.http4s.dsl.io._


object HttpPartnershipService {

  def routes(service: PartnershipService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "partnership" / "by-id" / UUIDVar(id) as _ =>
      service.partnerById(PartnerId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "partnership" / "by-user" / UUIDVar(id) as _ =>
      service.partnerByUserId(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "partnership" / "partner-for" /"post" / UUIDVar(id)  as _ =>
      service.partnerFor(PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
  }

}
