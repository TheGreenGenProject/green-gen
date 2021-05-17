package org.greengen.store.feed

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryFeedStore extends FeedStore[IO] {

  private[this] val feeds = new TrieMap[UserId, IndexedSeq[PostId]]

  override def getByUserId(id: UserId): IO[Option[IndexedSeq[PostId]]] =
    IO(feeds.get(id))

  override def getByUserIdOrElse(id: UserId, orElse: => IndexedSeq[PostId]): IO[IndexedSeq[PostId]] =
    IO(feeds.getOrElse(id, orElse))

  override def updateWith(id: UserId)(f: Option[IndexedSeq[PostId]] => Option[IndexedSeq[PostId]]): IO[Unit] =
    IO(feeds.updateWith(id)(f))

}
