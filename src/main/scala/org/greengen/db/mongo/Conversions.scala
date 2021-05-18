package org.greengen.db.mongo

import java.math.BigInteger

import cats.effect.{ContextShift, IO}
import org.greengen.core.UUID
import org.mongodb.scala.{Document, Observable}


object Conversions {

  // Observable to IO

  def unitIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Unit] =
    IO.fromFuture(IO(obs.toFuture())).map(_ => ())

  def firstIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[T] =
    IO.fromFuture(IO(obs.head()))

  def firstOptionIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Option[T]] =
    IO.fromFuture(IO(obs.headOption()))

  def toIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Seq[T]] =
    IO.fromFuture(IO(obs.toFuture()))

  def toListIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[List[T]] =
    IO.fromFuture(IO(obs.toFuture())).map(_.toList)

  // Helpers

  def bytes2Hex(bytes: List[Byte]): String =
    bytes.map(_.toHexString).mkString("")

  def hexToBytes(hex: String): List[Byte] =
    new BigInteger(hex, 16).toByteArray.toList

  def eitherPair[A,B,L](a: Either[L,A], b: Either[L,B]): Either[L,(A,B)] = for {
    aR <- a
    bR <- b
  } yield (aR,bR)

  def asUserId(doc: Document) =
    UUID.unsafeFrom(doc.getString("user_id"))

}
