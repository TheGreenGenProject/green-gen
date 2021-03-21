package org.greengen.core

import scala.util.Try

case class Base16 private(content: String)

object Base16 {

  def decodeFrom(b16: Base16): Array[Byte] =
    hex2bytes(b16.content)

  def decodeFrom(b16: String): Option[Array[Byte]] =
    if(b16.isEmpty || b16.length % 2 !=0) None
    else Try { hex2bytes(b16) }.toOption

  def encodeToBase16(bytes: Array[Byte]): Base16 =
    Base16(bytes2hex(bytes))

  def encodeToBase16(bytes: List[Byte]): Base16 =
    encodeToBase16(bytes.toArray)


  // Conversion function

  private[Base16] def hex2bytes(hex: String): Array[Byte] =
    hex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  private[Base16] def bytes2hex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString

}
