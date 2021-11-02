package org.greengen.store.follower

import cats.effect.IO
import cats.implicits._
import org.greengen.core.user.UserId


object CachedFollowerStore {
  def withCache(store: FollowerStore[IO]) =
    new CachedFollowerStore(new InMemoryFollowerStore, store)
}


private[follower] class CachedFollowerStore(cache: FollowerStore[IO],
                          persistent: FollowerStore[IO]) extends FollowerStore[IO] {

  override def getFollowersByUser(userId: UserId): IO[Set[UserId]] = for {
    cached <- cache.getFollowersByUser(userId)
    result <- {
      if(cached.nonEmpty) IO(cached)
      else persistent.getFollowersByUser(userId)
    }
    _ <- IO.whenA(cached.isEmpty && result.nonEmpty) {
      result.toList.map(cache.startFollowing(_, userId)).sequence.void
    }
  } yield result

  override def getFollowingByUser(userId: UserId): IO[Set[UserId]] = for {
    cached <- cache.getFollowingByUser(userId)
    result <- {
      if(cached.nonEmpty) IO(cached)
      else persistent.getFollowingByUser(userId)
    }
    _ <- IO.whenA(cached.isEmpty && result.nonEmpty) {
      result.toList.map(cache.startFollowing(userId, _)).sequence.void
    }
  } yield result

  override def startFollowing(src: UserId, dst: UserId): IO[Unit] = for {
    _      <- persistent.startFollowing(src, dst)
    // Update cache if non empty
    cached <- cache.getFollowingByUser(src)
    _      <- IO.whenA(cached.nonEmpty) { cache.startFollowing(src, dst) }
  } yield ()

  override def stopFollowing(src: UserId, dst: UserId): IO[Unit] = for {
    _      <- persistent.stopFollowing(src, dst)
    // Update cache if non empty
    cached <- cache.getFollowingByUser(src)
    _      <- IO.whenA(cached.nonEmpty) { cache.stopFollowing(src, dst) }
  } yield ()

}
