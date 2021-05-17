package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.{Clock, IOUtils, UTCTimestamp}
import org.greengen.core.follower.FollowerService
import org.greengen.core.notification.{NewFollowerNotification, Notification, NotificationService}
import org.greengen.core.user.{UserId, UserService}

import scala.collection.concurrent.TrieMap


@deprecated
class InMemoryFollowerService(clock: Clock,
                              userService: UserService[IO],
                              notificationService: NotificationService[IO])
  extends FollowerService[IO] {

  private[this] val followersByUser = new TrieMap[UserId, Set[UserId]]
  private[this] val followingByUser = new TrieMap[UserId, Set[UserId]]


  override def startFollowing(src: UserId, dst: UserId): IO[Unit] = for {
    _     <- checkUser(src)
    _     <- checkUser(dst)
    _     <- IO(updateMappings(src, dst, clock.now()))
    notif <- IO(Notification.from(clock, NewFollowerNotification(src)))
    _     <- notificationService.dispatch(notif, List(dst))
  } yield ()

  override def stopFollowing(src: UserId, dst: UserId): IO[Unit] = IO {
    removeMapping(src, dst)
  }

  override def followers(id: UserId): IO[Set[UserId]] = IO {
    followersByUser.getOrElse(id, Set())
  }

  override def countFollowers(id: UserId): IO[Int] = IO {
    followersByUser.getOrElse(id, Set()).size
  }

  override def following(id: UserId): IO[Set[UserId]] = IO {
    followingByUser.getOrElse(id, Set())
  }

  override def countFollowing(id: UserId): IO[Int] = IO {
    followingByUser.getOrElse(id, Set()).size
  }

  // Helpers

  private[this] def updateMappings(src: UserId, dst: UserId, timestamp: UTCTimestamp): Unit = {
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

  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
