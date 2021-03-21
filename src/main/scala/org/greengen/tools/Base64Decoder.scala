package org.greengen.tools

import cats.effect.{ExitCode, IO, IOApp}
import org.greengen.core.Base64

object Base64Decoder extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    _       <- putStr(">")
    b64     <- readLn()
    decoded <- IO(Base64.decodeFrom(b64))
    _       <- putStrLn(decoded.get.toList)
  } yield ExitCode.Success


  // Helpers

  def putStr[T](value: T) = IO(print(value))

  def putStrLn[T](value: T) = IO(println(value))

  def readLn() = IO(scala.io.StdIn.readLine())

}
