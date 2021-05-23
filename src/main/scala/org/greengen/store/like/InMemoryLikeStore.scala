package org.greengen.store.like

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

class InMemoryLikeStore extends LikeStore[IO] {

  private[this] val postLikes = new TrieMap[PostId, Set[UserId]]()

  override def getByPostId(id: PostId): IO[Set[UserId]] =
    IO(postLikes.getOrElse(id, Set()))

  override def countLikes(id: PostId): IO[Long] =
    getByPostId(id).map(_.size)

  override def addLike(userId: UserId, postId: PostId): IO[Unit] = IO {
    postLikes.updateWith(postId) {
      case Some(users) => Some(users + userId)
      case None => Some(Set(userId))
    }
  }

  override def removeLike(userId: UserId, postId: PostId): IO[Unit] = IO {
    postLikes.updateWith(postId) {
      case Some(users) => Some(users - userId)
      case None => Some(Set(userId))
    }
  }
}
