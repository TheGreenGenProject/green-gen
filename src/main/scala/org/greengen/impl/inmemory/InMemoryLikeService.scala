package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.{Clock, IOUtils}
import org.greengen.core.like.{Like, LikeService}
import org.greengen.core.notification.{Notification, NotificationService, PostLikedNotification}
import org.greengen.core.post.{PostId, PostService}
import org.greengen.core.user.{UserId, UserService}

import scala.collection.concurrent.TrieMap


class InMemoryLikeService(clock: Clock,
                          userService: UserService[IO],
                          notificationService: NotificationService[IO],
                          postService: PostService[IO]) extends LikeService[IO] {

  private[this] val postLikes = new TrieMap[PostId, Set[UserId]]()

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
    likers <- IO(postLikes.getOrElse(postId, Set()))
    liked  <- IO(likers.contains(userId))
  } yield liked

  override def countLikes(postId: PostId): IO[Like] =
    IO(Like(postLikes.getOrElse(postId, Set()).size))


  // Helpers

  private[this] def addLike(userId: UserId, postId: PostId): IO[Unit] = IO {
    postLikes.updateWith(postId) {
      case Some(users) => Some(users + userId)
      case None => Some(Set(userId))
    }
  }

  private[this] def removeLike(userId: UserId, postId: PostId): IO[Unit] = IO {
    postLikes.updateWith(postId) {
      case Some(users) => Some(users - userId)
      case None => Some(Set(userId))
    }
  }

  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
