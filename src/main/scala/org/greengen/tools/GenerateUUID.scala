package org.greengen.tools

import cats.effect.{ExitCode, IO, IOApp}
import org.greengen.core.{Base64, Hash, UUID}
import org.greengen.tools.Md5Hasher.{putStr, putStrLn, readLn}

object GenerateUUID extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    _   <- putStrLn(UUID.random().uuid)
  } yield ExitCode.Success


  // Helpers

  def putStrLn[T](value: T) = IO(println(value))

}
