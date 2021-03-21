package org.greengen.core

import scala.util.Try

case class UUID(uuid: String)

object UUID {

  def random(): UUID =
    UUID(java.util.UUID.randomUUID.toString)

  def unsafeFrom(uuid: String): UUID =
    UUID(java.util.UUID.fromString(uuid).toString)

  def from(uuid: String): Option[UUID] =
    Try(UUID(java.util.UUID.fromString(uuid).toString)).toOption

  def from(uuid: java.util.UUID): UUID =
    UUID(uuid.toString)

  def isValid(uuid: String): Boolean =
    Try(java.util.UUID.fromString(uuid)).isSuccess

}

