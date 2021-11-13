package org.greengen.store.pin

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.pin.PinnedPost
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


object CachedPinStore {
  def withCache(store: PinStore[IO]): PinStore[IO] =
    new CachedPinStore(store)
}


private[pin] class CachedPinStore(store: PinStore[IO]) extends PinStore[IO] {

  // Keeping missed cache
  private[this] val missCache = new TrieMap[(UserId,PostId), Unit]()

  override def addPin(userId: UserId, pinnedPost: PinnedPost): IO[Unit] = for {
    _ <- store.addPin(userId, pinnedPost)
    _ <- IO(missCache.remove((userId, pinnedPost.postId)))
  } yield ()

  override def removePin(userId: UserId, postId: PostId): IO[Unit] =
    store.removePin(userId, postId)

  override def isPinned(userId: UserId, postId: PostId): IO[Boolean] =
    for {
    alreadyMissed <- IO(missCache.contains(userId, postId))
    result        <- if(!alreadyMissed) store.isPinned(userId, postId)
                     else IO(false)
    _             <- IO.whenA(!result)(IO(missCache.put((userId, postId),())))
  } yield result

  override def getByUser(userId: UserId, page: Page): IO[List[PinnedPost]] =
    store.getByUser(userId, page)

}