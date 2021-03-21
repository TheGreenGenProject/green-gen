package org.greengen.http.user

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.user.{Pseudo, UserId, UserService}
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpUserService {

  def nonAuthRoutes(service: UserService[IO]) = HttpRoutes.of[IO] {
    // POST
    case POST -> Root / "user" / "create" :?
        PseudoQueryParamMatcher(pseudo) +&
        EmailHashQueryParamMatcher(emailHash) +&
        PasswordHashQueryParamMatcher(pwHash) +&
        ProfileIntroductionQueryParamMatcher(intro) =>
      service.create(pseudo, emailHash, pwHash, intro).flatMap(r => Ok(r.asJson))
  }

  def routes(service: UserService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "user" / "is-enabled" / UUIDVar(id) as _ =>
      service.isEnabled(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "user" / "by-pseudo" / pseudo as _ =>
      service.byPseudo(Pseudo(pseudo)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "user" / "by-prefix" / prefix as _ =>
      service.byPseudoPrefix(prefix).flatMap(r => Ok(r.asJson))
    case GET -> Root / "user" / "profile" / UUIDVar(id) as _ =>
      service.profile(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "users" / "active" as _ =>
      service.activeUsers().flatMap(r => Ok(r.asJson))
    case GET -> Root / "users" / "all" as _ =>
      service.users().flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "user" / "update" as userId =>
      service.updateProfile(userId, ???).flatMap(r => Ok(r.asJson))
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
