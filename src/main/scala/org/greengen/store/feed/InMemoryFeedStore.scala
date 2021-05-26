package org.greengen.store.feed

import cats.effect.IO
import org.greengen.core.{Page, PagedResult}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryFeedStore extends FeedStore[IO] {

  private[this] val feeds = new TrieMap[UserId, List[PostId]]

  override def hasPosts(userId: UserId): IO[Boolean] =
    IO(feeds.getOrElse(userId, List()).nonEmpty)

  override def mostRecentPost(userId: UserId): IO[Option[PostId]] =
    IO(feeds.get(userId).flatMap(_.headOption))

  override def getByUserId(id: UserId, page: Page): IO[List[PostId]] =
    IO(PagedResult.page(feeds.getOrElse(id, List()), page))

  override def addPost(userId: UserId, postId: PostId): IO[Unit] =  IO {
    feeds.updateWith(userId) {
      case Some(posts) => Some(postId +: posts)
      case None => Some(List(postId))
    }
  }

}
