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
