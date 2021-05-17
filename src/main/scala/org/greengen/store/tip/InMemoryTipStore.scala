package org.greengen.store.tip

import cats.effect.IO
import org.greengen.core.tip.{Tip, TipId}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

class InMemoryTipStore extends TipStore[IO] {

  private[this] val tips = new TrieMap[TipId, Tip]
  private[this] val authors = new TrieMap[UserId, Set[TipId]]

  override def getByTipId(id: TipId): IO[Option[Tip]] =
    IO(tips.get(id))

  override def getByAuthor(id: UserId): IO[Set[TipId]] =
    IO(authors.getOrElse(id, Set()))


  override def store(tip: Tip): IO[Unit] = for {
    _ <- indexById(tip)
    _ <- indexByAuthor(tip)
  } yield ()


  // Helpers

  private[this] def indexById(tip: Tip): IO[Unit] =
    IO(tips.put(tip.id, tip))


  private[this] def indexByAuthor(tip: Tip): IO[Unit] = IO {
    authors.updateWith(tip.author) {
      case Some(ids) => Some(ids + tip.id)
      case None => Some(Set(tip.id))
    }
  }
}
