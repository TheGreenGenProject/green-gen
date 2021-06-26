package org.greengen.core.registration

import scala.util.Random

case class ValidationCode(section1: Int, section2: Int, section3: Int)


object ValidationCode {

  val ValidationCodeRE = "(\\d){4}-(\\d){4}-(\\d){4}".r

  def generate(): ValidationCode =
    ValidationCode(Random.nextInt(1000), Random.nextInt(1000), Random.nextInt(1000))

  def format(vc: ValidationCode): String =
    "%04d-%04d-%04d".format(vc.section1, vc.section2, vc.section3)

  def from(code: String): Either[String, ValidationCode] = code match {
    case ValidationCodeRE(sec1, sec2, sec3) => Right(ValidationCode(sec1.toInt, sec2.toInt, sec3.toInt))
    case invalid                            => Left(s"Invalid validation code: ${invalid}")
  }

}
