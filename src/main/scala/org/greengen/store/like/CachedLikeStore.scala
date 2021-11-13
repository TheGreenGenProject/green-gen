package org.greengen.store.like

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


object CachedLikeStore {
  def withCache(store: LikeStore[IO]): LikeStore[IO] =
    new CachedLikeStore(store)
}

// Caching likes count
// FIXME This is an inaccurate cache - has several add/remove Like queries from a same user will be counted several times
// so the in-memory cache can get wrong - but the wrapped (persistent) cache will stay accurate
private[this] class CachedLikeStore(store: LikeStore[IO]) extends LikeStore[IO] {

  private[this] val countCache = new TrieMap[PostId, Long]

  override def hasUserLikedPost(userId: UserId, postId: PostId): IO[Boolean] =
    store.hasUserLikedPost(userId, postId)

  override def countLikes(postId: PostId): IO[Long] = for {
    cached <- IO(countCache.get(postId))
    result <- {
      if(cached.nonEmpty) IO(cached.get)
      else store.countLikes(postId)
    }
    _ <- IO.whenA(cached.isEmpty) {
      IO(countCache.put(postId, result))
    }
  } yield result

  override def addLike(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- store.addLike(userId, postId)
    _ <- IO {
      countCache.updateWith(postId) {
        case Some(prev) => Some(prev + 1)
        case None       => Some(1)
      }
    }
  } yield ()

  override def removeLike(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- store.removeLike(userId, postId)
    _ <- IO {
      countCache.updateWith(postId) {
        case Some(prev) => Some(math.max(0, prev - 1))
        case None       => None
      }
    }
  } yield ()
}
