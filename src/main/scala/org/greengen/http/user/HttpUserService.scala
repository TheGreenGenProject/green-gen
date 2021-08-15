package org.greengen.http.user

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.{Page, UUID}
import org.greengen.core.user.{Pseudo, UserId, UserService}
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpUserService {

  val PageSize = 50

  def routes(service: UserService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "user" / "is-enabled" / UUIDVar(id) as _ =>
      service.isEnabled(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "user" / "by-pseudo" / pseudo as _ =>
      service.byPseudo(Pseudo(pseudo)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "user" / "by-prefix" / prefix / IntVar(page) as _ =>
      service.byPseudoPrefix(prefix, Page(page, by=PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "user" / "profile" / UUIDVar(id) as _ =>
      service.profile(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "users" / "active" as _ =>
      service.activeUsers().flatMap(r => Ok(r.asJson))
    case GET -> Root / "users" / "all" as _ =>
      service.users().flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "user" / "delete" as userId =>
      service.delete(userId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "user" / "enable" :?
        UserIdQueryParamMatcher(id) +&
        ReasonQueryParamMatcher(reason) as _ =>
      service.enable(id, reason).flatMap(r => Ok(r.asJson))
    case POST -> Root / "user" / "disable" :?
        UserIdQueryParamMatcher(id) +&
        ReasonQueryParamMatcher(reason) as _ =>
      service.disable(id, reason).flatMap(r => Ok(r.asJson))
    case other =>
      NotFound()
  }

}
