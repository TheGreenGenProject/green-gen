package org.greengen.tools

import cats.effect.{ExitCode, IO, IOApp}
import org.greengen.core.Base64

object Base64Encoder extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    _       <- putStr(">")
    x       <- readLn()
    encoded <- IO(Base64.encodeToBase64(x.getBytes("UTF-8")))
    _       <- putStrLn(encoded)
  } yield ExitCode.Success


  // Helpers

  def putStr[T](value: T) = IO(print(value))

  def putStrLn[T](value: T) = IO(println(value))

  def readLn() = IO(scala.io.StdIn.readLine())

}
