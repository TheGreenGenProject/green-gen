package org.greengen.store.wall

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

trait WallStore[F[_]] {

  def getByUserId(id: UserId): F[Option[IndexedSeq[PostId]]]

  def updateWith(id: UserId)(f: Option[IndexedSeq[PostId]] => Option[IndexedSeq[PostId]]): F[Unit]

}
