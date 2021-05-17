package org.greengen.store.like

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait LikeStore[F[_]] extends Store[F] {

  def getByPostId(id: PostId): F[Option[Set[UserId]]]

  def getByPostIdOrElse(id: PostId, orElse: => Set[UserId]): F[Set[UserId]]

  def updateWith(id: PostId)(f: Option[Set[UserId]] => Option[Set[UserId]]): F[Unit]

}
