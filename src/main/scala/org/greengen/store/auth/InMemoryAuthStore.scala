package org.greengen.store.auth

import cats.effect.IO
import org.greengen.core.Token
import org.greengen.core.auth.Auth

import scala.collection.concurrent.TrieMap


class InMemoryAuthStore extends AuthStore[IO] {

  private[this] val byToken = new TrieMap[Token, Auth]

  override def put(token: Token, auth: Auth): IO[Unit] =
    IO(byToken.put(token, auth))

  override def get(token: Token): IO[Option[Auth]] =
    IO(byToken.get(token))

  override def remove(token: Token): IO[Unit] =
    IO(byToken.remove(token))

}
