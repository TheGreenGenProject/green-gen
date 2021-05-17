package org.greengen.store.feed

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


trait FeedStore[F[_]] {

  def getByUserId(id: UserId): F[Option[IndexedSeq[PostId]]]

  def getByUserIdOrElse(id: UserId, orElse: => IndexedSeq[PostId]): F[IndexedSeq[PostId]]

  def updateWith(id: UserId)(f: Option[IndexedSeq[PostId]] => Option[IndexedSeq[PostId]]): F[Unit]

}