package org.greengen.impl.auth

import cats.effect.IO
import org.greengen.core.auth.{Auth, AuthService, Authenticated, NotAuthenticated}
import org.greengen.core.user.UserService
import org.greengen.core.{Clock, Hash, Token}
import org.greengen.store.auth.AuthStore


class AuthServiceImpl(authStore: AuthStore[IO])(clock: Clock,
                      userService: UserService[IO])
  extends AuthService[IO] {

  // Token is valid for 4 hours
  val TokenValidity = 4 * 3600 * 1000L

  override def authenticate(email: Hash, password: Hash): IO[Auth] = for {
    userInfo <- userService.byHash(email, password)
    _ <- IO.fromOption(userInfo.filter(_._1.enabled))(new RuntimeException("User doesn't exist or is disabled"))
    token <- IO(Token.newToken)
    auth  <- IO(userInfo.fold[Auth]
               (NotAuthenticated)
               (user => Authenticated(token, user._1.id, clock.now().plusMillis(TokenValidity))))
    _ <- IO.whenA(auth.isAuthenticated)(authStore.put(token,auth))
    _ <- IO(println(s"Authentication for email=$email and pw=$password = $auth"))
  } yield auth

  override def logoff(token: Token): IO[Unit] =
    authStore.remove(token)

  override def isAuthenticated(token: Token): IO[Boolean] = for {
    auth <- authFor(token)
    // Taking the chance to do some clean-up if in case auth is not valid anymore
    _ <- IO.whenA(!auth.isAuthenticated)(authStore.remove(token))
  } yield auth.isAuthenticated

  override def authFor(token: Token): IO[Auth] = for {
    auth    <- authStore.get(token)
    updated <- IO(auth.map(_.update(clock)).getOrElse(NotAuthenticated))
  } yield updated

}
