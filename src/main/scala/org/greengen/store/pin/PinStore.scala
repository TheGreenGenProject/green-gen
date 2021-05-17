package org.greengen.store.pin

import org.greengen.core.Page
import org.greengen.core.pin.PinnedPost
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.store.Store

trait PinStore[F[_]] extends Store[F] {

 def addPin(userId: UserId, pinnedPost: PinnedPost): F[Unit]

  def removePin(userId: UserId, postId: PostId): F[Unit]

  def isPinned(userId: UserId, postId: PostId): F[Boolean]

  // List of pinned posts, sorted by pin timestamp desc
  def getByUser(userId: UserId, page: Page): F[List[PinnedPost]]
}
