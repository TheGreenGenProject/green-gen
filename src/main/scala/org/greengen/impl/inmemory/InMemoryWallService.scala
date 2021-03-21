package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.feed.FeedService
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.wall.{Wall, WallService}
import org.greengen.core.{IOUtils, Page, PagedResult}

import scala.collection.concurrent.TrieMap


class InMemoryWallService(userService: UserService[IO],
                          feedService: FeedService[IO])
  extends WallService[IO] {

  private[this] val walls = new TrieMap[UserId, IndexedSeq[PostId]]

  override def wall(userId: UserId, page: Page): IO[Wall] = for {
    _ <- checkUser(userId)
    content <- IO(getWallPage(userId, page))
  } yield Wall(userId, content.toList)

  override def addToWall(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- IO(addPostToWall(userId, postId))
    _ <- feedService.addToFollowersFeed(userId, postId)
  } yield ()


  // Helpers

  private[this] def getWallPage(userId: UserId, page: Page): IndexedSeq[PostId] =
    PagedResult.page(walls.getOrElse(userId, IndexedSeq()), page)

  private[this] def addPostToWall(userId: UserId, postId: PostId): Unit =
    walls.updateWith(userId) {
      case Some(posts) => Some(postId +: posts)
      case None => Some(IndexedSeq(postId))
    }

  private[this] def checkUser(id: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(id)
    _ <- IOUtils.check(enabled, s"User $id is not enabled")
  } yield ()

}
