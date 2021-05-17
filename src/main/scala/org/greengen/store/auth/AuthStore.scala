package org.greengen.store.auth

import org.greengen.core.Token
import org.greengen.core.auth.Auth
import org.greengen.store.Store

trait AuthStore[F[_]] extends Store[F] {

  def put(token: Token, auth: Auth): F[()]

  def get(token: Token): F[Option[Auth]]

  def remove(token: Token): F[()]

}
