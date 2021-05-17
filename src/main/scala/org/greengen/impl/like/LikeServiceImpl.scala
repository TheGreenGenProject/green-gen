package org.greengen.impl.like

import cats.effect.IO
import org.greengen.core.like.{Like, LikeService}
import org.greengen.core.notification.{Notification, NotificationService, PostLikedNotification}
import org.greengen.core.post.{PostId, PostService}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils}
import org.greengen.store.like.LikeStore


class LikeServiceImpl(likeStore: LikeStore[IO])
                     (clock: Clock,
                      userService: UserService[IO],
                      notificationService: NotificationService[IO],
                      postService: PostService[IO]) extends LikeService[IO] {

  override def like(userId: UserId, postId: PostId): IO[Unit] = for {
    _         <- checkUser(userId)
    maybePost <- postService.byId(postId)
    post      <- IOUtils.from(maybePost, s"Post $postId doesn't exist")
    _         <- addLike(userId, postId)
    author    <- IO(post.author)
    notif     <- IO(Notification.from(clock, PostLikedNotification(postId, userId)))
    _         <- notificationService.dispatch(notif, List(author))
  } yield ()

  override def unlike(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- removeLike(userId, postId)
  } yield ()

  override def isLiked(userId: UserId, postId: PostId): IO[Boolean] = for {
    _      <- checkUser(userId)
    likers <- likeStore.getByPostIdOrElse(postId, Set())
    liked  <- IO(likers.contains(userId))
  } yield liked

  override def countLikes(postId: PostId): IO[Like] = for {
   users <- likeStore.getByPostIdOrElse(postId, Set())
  } yield Like(users.size)


  // Helpers

  private[this] def addLike(userId: UserId, postId: PostId): IO[Unit] =
    likeStore.updateWith(postId) {
      case Some(users) => Some(users + userId)
      case None => Some(Set(userId))
    }

  private[this] def removeLike(userId: UserId, postId: PostId): IO[Unit] =
    likeStore.updateWith(postId) {
      case Some(users) => Some(users - userId)
      case None => Some(Set(userId))
    }

  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
