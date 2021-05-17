package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.tip.{Tip, TipId, TipService}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Source}

import scala.collection.concurrent.TrieMap


@deprecated
class InMemoryTipService(clock: Clock, userService: UserService[IO]) extends TipService[IO] {

  private[this] val tips = new TrieMap[TipId, Tip]
  private[this] val authors = new TrieMap[UserId, Set[TipId]]

  override def create(author: UserId, content: String, sources: List[Source]): IO[TipId] = for {
    _     <- checkUser(author)
    tipId <- IO(TipId.newId())
    tip   <- IO(Tip(tipId, author, content, clock.now(), sources))
    _     <- IO(indexById(tip))
    _     <- IO(indexByAuthor(tip))
  } yield tipId

  override def byId(tipId: TipId): IO[Option[Tip]] = IO {
    tips.get(tipId)
  }

  override def byAuthor(author: UserId): IO[Set[TipId]] = IO {
    authors.getOrElse(author, Set())
  }


  // Indexing

  private[this] def indexById(tip: Tip): Unit =
    tips.put(tip.id, tip)


  private[this] def indexByAuthor(tip: Tip): Unit =
    authors.updateWith(tip.author) {
      case Some(ids) => Some(ids + tip.id)
      case None => Some(Set(tip.id))
    }

  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()


}
