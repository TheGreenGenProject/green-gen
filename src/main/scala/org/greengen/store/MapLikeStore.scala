package org.greengen.store

import cats.effect.IO

import scala.collection.concurrent.TrieMap

// Store interface matching a basic Map implementation
trait MapLikeStore[F[_],K,V] extends Store[F] {
  def getBy(id: K): IO[Option[V]]
  def getByOrElse(id: K, orElse: => V): IO[V]
  def updateWith(id: K)(f: Option[V] => Option[V]): IO[Unit]
}

// Default in-memory implementation of a store backed by a Map
class InMemoryMapLikeIOStore[K, V] extends Store[IO] {

  private[this] val content = new TrieMap[K, V]()

  def getBy(id: K): IO[Option[V]] =
    IO(content.get(id))

  def getByOrElse(id: K, orElse: => V): IO[V] =
    IO(content.getOrElse(id, orElse))

  def updateWith(id: K)(f: Option[V] => Option[V]): IO[Unit] =
    IO(content.updateWith(id)(f))

}
