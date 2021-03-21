package org.greengen.core.auth

import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Token, UTCTimestamp}

// Authentication
sealed trait Auth {

  def isAuthenticated: Boolean
  def isRejected: Boolean

  def fold[T](nonAuth: => T)(auth: Authenticated => T): T
  def toOption: Option[Authenticated]
  def toEither[T](left: => T): Either[T, Authenticated]

  def isValid(clock: Clock): Boolean
  def update(clock: Clock): Auth =
    if(isValid(clock)) this else NotAuthenticated

}

case class Authenticated(
  token: Token,
  user: UserId,
  validUntil: UTCTimestamp) extends Auth {

  override def isAuthenticated: Boolean = true
  override def isRejected: Boolean = false

  override def fold[T](nonAuth: => T)(auth: Authenticated => T): T = auth(this)
  override def toOption: Option[Authenticated] = Some(this)
  override def toEither[T](left: => T): Either[T, Authenticated] = Right(this)

  override def isValid(clock: Clock): Boolean =
    clock.now().value <= validUntil.value
}

case object NotAuthenticated extends Auth {
  override def isAuthenticated: Boolean = false
  override def isRejected: Boolean = true

  override def fold[T](nonAuth: => T)(auth: Authenticated => T): T = nonAuth
  override def toOption: Option[Authenticated] = None
  override def toEither[T](left: => T): Either[T, Authenticated] = Left(left)

  override def isValid(clock: Clock): Boolean = false
}
