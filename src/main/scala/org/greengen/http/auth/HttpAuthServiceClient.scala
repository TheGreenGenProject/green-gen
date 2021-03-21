package org.greengen.http.auth

import cats.effect.IO
import org.greengen.core.{Hash, Token}
import org.greengen.core.auth.{Auth, AuthService}
import org.greengen.http.HttpHelper.post
import org.http4s.Uri
import org.http4s.client.Client
import org.greengen.http.JsonDecoder._



class HttpAuthServiceClient(httpClient: Client[IO], root: Uri) extends AuthService[IO] {

  override def authenticate(email: Hash, password: Hash): IO[Auth] =
    httpClient.expect[Auth](post(root / "auth" / "authenticate",
      "email-hash" -> email,
      "password-hash" -> password))

  override def logoff(token: Token): IO[Unit] =
    httpClient.expect[Unit](post(root / "auth" / "logoff",
      "token" -> token.value.uuid))

  override def isAuthenticated(token: Token): IO[Boolean] =
    httpClient.expect[Boolean](root / "auth" / "is-authenticated" / token.value.uuid)

  override def authFor(token: Token): IO[Auth] =
    httpClient.expect[Auth](root / "auth" / "auth-for" / token.value.uuid)

}
