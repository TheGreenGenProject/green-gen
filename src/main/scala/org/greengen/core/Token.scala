package org.greengen.core


case class Token(value: UUID)

object Token {

  def newToken = Token(UUID.random())

  def unsafeFrom(str: String): Token =
    Token(UUID.unsafeFrom(str))

  def from(str: String): Option[Token] =
    UUID.from(str).map(Token(_))

  def from(uuid: java.util.UUID): Token =
    Token(UUID.from(uuid))

}
