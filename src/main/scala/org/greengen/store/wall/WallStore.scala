package org.greengen.store.wall

import org.greengen.core.Page
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

trait WallStore[F[_]] {

  def getByUserId(userId: UserId, page: Page): F[List[PostId]]

  def addPost(id: UserId, postId: PostId): F[Unit]

}
