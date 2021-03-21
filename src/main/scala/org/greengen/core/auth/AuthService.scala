package org.greengen.core.auth

import org.greengen.core.{Hash, Token}

trait AuthService[F[_]] {

  def authenticate(email: Hash, password: Hash): F[Auth]
  def logoff(token: Token): F[Unit]

  def isAuthenticated(token: Token): F[Boolean]
  def authFor(token: Token):F[Auth]

}
