package org.greengen.core

import java.net.URL

import scala.util.Try

case class Url(url: String) {
  def toURI = new URL(url).toURI
}

object Url {

  def apply(url: String): Url =
    Url(url)

  def fromString(url: String): Try[Url] =
    Try(Url(new URL(url).toURI.toString))
}
