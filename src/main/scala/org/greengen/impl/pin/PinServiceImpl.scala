package org.greengen.impl.pin

import cats.effect.IO
import org.greengen.core.pin.{PinService, PinnedPost}
import org.greengen.core.post.{PostId, PostService}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Page, PagedResult}
import org.greengen.store.pin.PinStore

import scala.collection.concurrent.TrieMap

class PinServiceImpl(pinStore: PinStore[IO])
                    (clock: Clock,
                     userService: UserService[IO],
                     postService: PostService[IO]) extends PinService[IO] {

  override def pin(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- checkPost(postId)
    _ <- pinStore.addPin(userId, PinnedPost(postId, clock.now()))
  } yield ()

  override def unpin(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- pinStore.removePin(userId, postId)
  } yield ()

  override def isPinned(userId: UserId, postId: PostId): IO[Boolean] = for {
    _        <- checkUser(userId)
    isPinned <- pinStore.isPinned(userId, postId)
  } yield isPinned

  override def byUser(userId: UserId, page: Page): IO[List[PinnedPost]] = for {
    _      <- checkUser(userId)
    pinned <- pinStore.getByUser(userId, page)
  } yield pinned


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
