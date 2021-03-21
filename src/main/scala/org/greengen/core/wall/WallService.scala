package org.greengen.core.wall

import org.greengen.core.Page
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

trait WallService[F[_]] {

  def wall(userId: UserId, page: Page): F[Wall]

  def addToWall(userId: UserId, post: PostId): F[Unit]

}
