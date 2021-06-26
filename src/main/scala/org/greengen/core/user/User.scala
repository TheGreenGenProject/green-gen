package org.greengen.core.user

import org.greengen.core.{Hash, UTCTimestamp, UUID}

case class UserId(value: UUID)
case class Pseudo(value: String)

case class User(
  id: UserId,
  emailHash: Hash,
  passwordHash: Hash,
  enabled: Boolean
)
case class Profile(
  id: UserId,
  pseudo: Pseudo,
  since: UTCTimestamp,
  intro: Option[String],
  verified: Boolean = false
)

object UserId {
  def newId() = UserId(UUID.random())
}

object Pseudo {
  val PseudoRE = "\\w(\\w|\\d|_){2,14}".r
  def from(candidate: String): Either[String, Pseudo] = candidate match {
    case PseudoRE(_) => Right(Pseudo(candidate))
    case _           => Left("Invalid pseudo. A pseudo must be between 3 and 15 characters, must start with a letter, and must contains only letters, numbers and underscores.")
  }
}
