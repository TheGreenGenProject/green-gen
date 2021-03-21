package org.greengen.core.pin

import org.greengen.core.Page
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

trait PinService [F[_]] {

  def pin(userId: UserId, postId: PostId): F[Unit]

  def unpin(userId: UserId, postId: PostId): F[Unit]

  def isPinned(userId: UserId, postId: PostId): F[Boolean]

  def byUser(userId: UserId, page: Page): F[List[PinnedPost]]

}