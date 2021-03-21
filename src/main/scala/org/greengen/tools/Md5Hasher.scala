package org.greengen.tools

import cats.effect.{ExitCode, IO, IOApp}
import org.greengen.core.{Base64, Hash}

object Md5Hasher extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    _       <- putStr(">")
    x       <- readLn()
    md5     <- IO.fromOption(Hash.md5(x))(new RuntimeException("Couldn't compute MD5"))
    _       <- putStrLn(s"MD5 Bytes = ${md5.bytes.mkString("|")}")
    b64     <- IO(Base64.encodeToBase64(md5.bytes))
    _       <- putStrLn(s"Encoded   = '${b64.content}'")
    decoded <- IO(Base64.decodeFrom(b64))
    _       <- putStrLn(s"Decoded   = ${decoded.mkString("|")}")
  } yield ExitCode.Success


  // Helpers

  def putStr[T](value: T) = IO(print(value))

  def putStrLn[T](value: T) = IO(println(value))

  def readLn() = IO(scala.io.StdIn.readLine())
}
