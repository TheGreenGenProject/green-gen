package org.greengen.core.like

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

trait LikeService[F[_]] {

  def like(userId: UserId, postId: PostId): F[Unit]

  def unlike(userId: UserId, postId: PostId): F[Unit]

  def isLiked(userId: UserId, postId: PostId): F[Boolean]

  def countLikes(postId: PostId): F[Like]

}
