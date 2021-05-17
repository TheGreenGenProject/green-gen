package org.greengen.impl.follower

import cats.effect.IO
import org.greengen.core.follower.FollowerService
import org.greengen.core.notification.{NewFollowerNotification, Notification, NotificationService}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils}
import org.greengen.store.follower.FollowerStore


class FollowerServiceImpl(followerStore: FollowerStore[IO])
                         (clock: Clock,
                          userService: UserService[IO],
                          notificationService: NotificationService[IO])
  extends FollowerService[IO] {


  override def startFollowing(src: UserId, dst: UserId): IO[Unit] = for {
    _     <- checkUser(src)
    _     <- checkUser(dst)
    _     <- followerStore.startFollowing(src, dst)
    notif <- IO(Notification.from(clock, NewFollowerNotification(src)))
    _     <- notificationService.dispatch(notif, List(dst))
  } yield ()

  override def stopFollowing(src: UserId, dst: UserId): IO[Unit] =
    followerStore.stopFollowing(src, dst)

  override def followers(id: UserId): IO[Set[UserId]] =
    followerStore.getFollowersByUser(id)

  override def countFollowers(id: UserId): IO[Int] = for {
    allFollowers <- followers(id)
  } yield allFollowers.size

  override def following(id: UserId): IO[Set[UserId]] =
    followerStore.getFollowingByUser(id)

  override def countFollowing(id: UserId): IO[Int] = for {
    allFollowing <- following(id)
  } yield allFollowing.size

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
