package org.greengen.core

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.util.Try


case class Hash private[Hash](bytes: List[Byte]) {
  override def toString() = Base16.encodeToBase16(bytes).content
}

object Hash {

  val MD5 = "MD5"
  val UTF8 = StandardCharsets.UTF_8

  def md5(s: String): Option[Hash] =
    md5(s.getBytes(UTF8))

  def md5(bytes: Array[Byte]): Option[Hash] =
    Try(MessageDigest.getInstance(MD5).digest(bytes).toList)
      .toOption
      .map(Hash(_))

  def safeFrom(bytes: Array[Byte]): Hash =
    Hash(bytes.toList)

}


