package org.greengen.http.auth

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.Token
import org.greengen.core.auth.AuthService
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpAuthService {


  def nonAuthRoutes(service: AuthService[IO]) = HttpRoutes.of[IO] {
    // POST
    case POST -> Root / "auth" / "authenticate" :?
      EmailHashQueryParamMatcher(emailHash) +&
      PasswordHashQueryParamMatcher(passwordHash) =>
      service.authenticate(emailHash, passwordHash).flatMap(r => Ok(r.asJson))
  }

  def routes(service: AuthService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "auth" / "is-authenticated" / UUIDVar(token) as _ =>
      service.isAuthenticated(Token.from(token)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "auth" / "auth-for" / UUIDVar(token) as _ =>
      service.authFor(Token.from(token)).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "logoff" :? TokenQueryParamMatcher(token) as _ =>
      service.logoff(token).flatMap(r => Ok(r.asJson))
  }

}
