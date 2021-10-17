package org.greengen.core

import cats.effect.IO

import scala.util.Try


object IOUtils {

  def lift[T](t: T): IO[T] = IO.pure(t)

  def check(b: Boolean, err: String): IO[Unit] =
    if(b) IO.pure(()) else IO.raiseError(new RuntimeException(err))

  def from[T](maybe: Option[T], err: String): IO[T] =
    maybe.fold(IO.raiseError[T](new RuntimeException(err)))(IO.pure)

  def from[T](either: Either[Throwable, T]): IO[T] =
    either.fold(err => IO.raiseError[T](err), IO.pure(_))

  def from[T](attempt: Try[T]): IO[T] =
    attempt.fold(err => IO.raiseError[T](err), IO.pure(_))

  def defined[T](maybe: IO[Option[T]], err: String): IO[T] =
    maybe.flatMap(_.fold(IO.raiseError[T](new RuntimeException(err)))(IO.pure))

}
