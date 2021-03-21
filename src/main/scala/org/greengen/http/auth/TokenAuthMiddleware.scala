package org.greengen.http.auth

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.greengen.core.Token
import org.greengen.core.auth.{AuthService, Authenticated, NotAuthenticated}
import org.greengen.core.user.UserId
import org.http4s._
import org.http4s.server.AuthMiddleware
import org.http4s.util.CaseInsensitiveString


object TokenAuthMiddleware {

  val AuthorizationHeader = "Authorization"

  // Http4s auth middleware delegating to the authService
  def authMiddleware(authService: AuthService[IO]): AuthMiddleware[IO, UserId] =
    AuthMiddleware(authUser(authService), onAuthFailure)

  private[this] def authUser(authService: AuthService[IO]): Kleisli[IO, Request[IO], Either[NotAuthenticated.type, UserId]] =
    Kleisli { request: Request[IO] =>
      val header: Option[Header] = request.headers.get(CaseInsensitiveString(AuthorizationHeader))
      header match {
        case Some(maybeToken) => userIdFromToken(authService)(Token.from(maybeToken.value))
        case None             => IO.pure(Left(NotAuthenticated))
      }
    }

  private[this] def userIdFromToken(authService: AuthService[IO])(maybeToken: Option[Token]): IO[Either[NotAuthenticated.type, UserId]] =
    maybeToken match {
      case Some(tk) => authService.authFor(tk).map {
        case Authenticated(_, user, _) => Right(user)
        case NotAuthenticated          => Left(NotAuthenticated)
      }
      case None     => IO.pure(Left(NotAuthenticated))
    }

  private[this] def onAuthFailure: AuthedRoutes[NotAuthenticated.type, IO] = Kleisli {
    req: AuthedRequest[IO, NotAuthenticated.type] =>
      req.req match {
        case _ =>
          println(s"Unauthorized: $req")
          OptionT.pure[IO](Response[IO](status = Status.Unauthorized)) // 401
      }
   }
}
