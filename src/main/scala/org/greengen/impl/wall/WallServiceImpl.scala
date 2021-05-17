package org.greengen.impl.wall

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.wall.{Wall, WallService}
import org.greengen.core.{IOUtils, Page, PagedResult}
import org.greengen.store.wall.WallStore


class WallServiceImpl(wallStore: WallStore[IO])
                     (userService: UserService[IO])
  extends WallService[IO] {

  override def wall(userId: UserId, page: Page): IO[Wall] = for {
    _       <- checkUser(userId)
    content <- getWallPage(userId, page)
  } yield Wall(userId, content.toList)

  override def addToWall(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- addPostToWall(userId, postId)
  } yield ()


  // Helpers

  private[this] def getWallPage(userId: UserId, page: Page): IO[IndexedSeq[PostId]] = for {
    posts <- wallStore.getByUserId(userId).map(_.getOrElse(IndexedSeq()))
    res   <- IO(PagedResult.page(posts, page))
  } yield res

  private[this] def addPostToWall(userId: UserId, postId: PostId): IO[Unit] =
    wallStore.updateWith(userId) {
      case Some(posts) => Some(postId +: posts)
      case None => Some(IndexedSeq(postId))
    }

  private[this] def checkUser(id: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(id)
    _ <- IOUtils.check(enabled, s"User $id is not enabled")
  } yield ()

}
