package org.greengen.store.like

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

class InMemoryLikeStore extends LikeStore[IO] {

  private[this] val postLikes = new TrieMap[PostId, Set[UserId]]()

  override def getByPostId(id: PostId): IO[Option[Set[UserId]]] =
    IO(postLikes.get(id))

  override def getByPostIdOrElse(id: PostId, orElse: => Set[UserId]): IO[Set[UserId]] =
    IO(postLikes.getOrElse(id, orElse))

  override def updateWith(id: PostId)(f: Option[Set[UserId]] => Option[Set[UserId]]): IO[Unit] =
    IO(postLikes.updateWith(id)(f))

}
