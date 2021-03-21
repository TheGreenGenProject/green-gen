package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.{Clock, IOUtils, Page, PagedResult}
import org.greengen.core.pin.{PinService, PinnedPost}
import org.greengen.core.post.{PostId, PostService}
import org.greengen.core.user.{UserId, UserService}

import scala.collection.concurrent.TrieMap

class InMemoryPinService(clock: Clock,
                         userService: UserService[IO],
                         postService: PostService[IO]) extends PinService[IO] {

  private[this] val users = new TrieMap[UserId, Map[PostId,PinnedPost]]

  override def pin(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- checkPost(postId)
    _ <- IO(indexByUser(userId, PinnedPost(postId, clock.now())))
  } yield ()

  override def unpin(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    pinned <- IO(users.getOrElse(userId, Map()))
    _ <- IO(users.update(userId, pinned - postId))
  } yield ()


  override def isPinned(userId: UserId, postId: PostId): IO[Boolean] = for {
    _ <- checkUser(userId)
    pinned <- IO(users.getOrElse(userId, Map()))
    isPinned <- IO(pinned.contains(postId))
  } yield isPinned

  override def byUser(userId: UserId, page: Page): IO[List[PinnedPost]] = for {
    _ <- checkUser(userId)
    pinned <- IO(users.getOrElse(userId, Map()))
    sorted <- IO(pinned.values.toList.sortBy { _.timestamp.value }.reverse)
  } yield PagedResult.page(sorted, page)


  // Indexer

  private[this] def indexByUser(userId: UserId, pin: PinnedPost): Unit =
    users.updateWith(userId) {
      case Some(pinned) => Some(pinned + (pin.postId -> pin))
      case None => Some(Map(pin.postId -> pin))
    }

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkPost(postId: PostId): IO[Unit] = for {
    maybePost <- postService.byId(postId)
    _ <- IOUtils.from(maybePost, s"Post $postId doesn't exist")
  } yield ()

}
