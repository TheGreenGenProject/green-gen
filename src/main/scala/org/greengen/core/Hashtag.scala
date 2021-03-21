package org.greengen.core

case class Hashtag private(value: String) extends AnyVal

object Hashtag {

  def apply(tag: String) =
    new Hashtag(tag.trim.toLowerCase())

  def format(tag: Hashtag) =
    s"#${tag.value.toLowerCase}"
}
