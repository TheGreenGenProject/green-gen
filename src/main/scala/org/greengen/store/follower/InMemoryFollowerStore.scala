package org.greengen.store.follower

import cats.effect.IO
import org.greengen.core.{Clock, UTCTimestamp}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryFollowerStore() extends FollowerStore[IO] {

  private[this] val followersByUser = new TrieMap[UserId, Set[UserId]]
  private[this] val followingByUser = new TrieMap[UserId, Set[UserId]]


  override def getFollowersByUser(userId: UserId): IO[Set[UserId]] =
    IO(followersByUser.getOrElse(userId, Set()))

  override def getFollowingByUser(userId: UserId): IO[Set[UserId]] =
    IO(followingByUser.getOrElse(userId, Set()))

  override def startFollowing(src: UserId, dst: UserId): IO[Unit] =
    IO(updateMappings(src, dst))

  override def stopFollowing(src: UserId, dst: UserId): IO[Unit] =
    IO(removeMapping(src, dst))


  // Helpers

  private[this] def updateMappings(src: UserId, dst: UserId): Unit = {
    followersByUser.updateWith(dst) {
      case Some(following) => Some(following + src)
      case None => Some(Set(src))
    }
    followingByUser.updateWith(src) {
      case Some(follower) => Some(follower + dst)
      case None => Some(Set(dst))
    }
  }

  private[this] def removeMapping(src: UserId, dst: UserId): Unit = {
    followersByUser.updateWith(dst) {
      case Some(following) => Some(following - src)
      case None => None
    }
    followingByUser.updateWith(src) {
      case Some(follower) => Some(follower - dst)
      case None => None
    }
  }

}
