package org.greengen.store.like

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait LikeStore[F[_]] extends Store[F] {

  def getByPostId(id: PostId): F[Set[UserId]]

  def hasUserLikedPost(userId: UserId, postId: PostId): F[Boolean]

  def countLikes(id: PostId): F[Long]

  def addLike(userId: UserId, postId: PostId): F[Unit]

  def removeLike(userId: UserId, postId: PostId): F[Unit]

}
