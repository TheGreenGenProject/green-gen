package org.greengen.store.pin

import cats.effect.IO
import org.greengen.core.{Clock, Page, PagedResult}
import org.greengen.core.pin.PinnedPost
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

class InMemoryPinStore extends PinStore[IO] {

  private[this] val users = new TrieMap[UserId, Map[PostId,PinnedPost]]


  override def addPin(userId: UserId, pinnedPost: PinnedPost): IO[Unit] =
    IO(indexByUser(userId,pinnedPost))

  override def removePin(userId: UserId, postId: PostId): IO[Unit] = for {
    pinned <- IO(users.getOrElse(userId, Map()))
    _      <- IO(users.update(userId, pinned - postId))
  } yield ()

  override def isPinned(userId: UserId, postId: PostId): IO[Boolean] =
    IO(users.getOrElse(userId, Map()).contains(postId))

  override def getByUser(userId: UserId, page: Page): IO[List[PinnedPost]] = for {
    pinned <- IO(users.getOrElse(userId, Map()))
    sorted <- IO(pinned.values.toList.sortBy { _.timestamp.value }.reverse)
  } yield PagedResult.page(sorted, page)

  // Indexer

  private[this] def indexByUser(userId: UserId, pin: PinnedPost): Unit =
    users.updateWith(userId) {
      case Some(pinned) => Some(pinned + (pin.postId -> pin))
      case None => Some(Map(pin.postId -> pin))
    }
}
