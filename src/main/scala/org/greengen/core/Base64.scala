package org.greengen.core

import scala.util.Try

case class Base64 private(content: String)

object Base64 {

  def decodeFrom(b64: Base64): Array[Byte] =
    java.util.Base64.getDecoder().decode(b64.content)

  def decodeFrom(b64: String): Option[Array[Byte]] = Try {
    val processed = b64.replace(' ','+') // FIXME needed when passed through postman URL
    java.util.Base64.getDecoder().decode(processed)
  }.toOption

  def encodeToBase64(bytes: Array[Byte]): Base64 =
    Base64(java.util.Base64.getEncoder().encodeToString(bytes))

  def encodeToBase64(bytes: List[Byte]): Base64 =
    encodeToBase64(bytes.toArray)

}
