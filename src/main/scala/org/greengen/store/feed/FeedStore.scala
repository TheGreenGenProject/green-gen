package org.greengen.store.feed

import org.greengen.core.Page
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


trait FeedStore[F[_]] {

  def hasPosts(userId: UserId): F[Boolean]

  def mostRecentPost(userId: UserId): F[Option[PostId]]

  def getByUserId(id: UserId, page: Page): F[List[PostId]]

  def addPost(userId: UserId, postId: PostId): F[Unit]

}