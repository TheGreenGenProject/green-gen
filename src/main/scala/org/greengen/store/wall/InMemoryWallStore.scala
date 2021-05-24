package org.greengen.store.wall

import cats.effect.IO
import org.greengen.core.{Page, PagedResult}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryWallStore extends WallStore[IO] {

  private[this] val walls = new TrieMap[UserId, List[PostId]]

  override def getByUserId(id: UserId, page: Page): IO[List[PostId]] =
    IO(PagedResult.page(walls.getOrElse(id, List()), page))

  override def addPost(userId: UserId, postId: PostId): IO[Unit] = IO {
    walls.updateWith(userId) {
      case Some(posts) => Some(postId :: posts)
      case None => Some(List(postId))
    }
  }

}
