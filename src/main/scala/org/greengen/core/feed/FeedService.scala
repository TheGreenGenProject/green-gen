package org.greengen.core.feed

import org.greengen.core.{Page, UTCTimestamp}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

trait FeedService[F[_]] {

  def feed(userId: UserId, page: Page): F[Feed]

  def addToFeed(userId: UserId, postId: PostId): F[Unit]

  def addToFollowersFeed(userId: UserId, postId: PostId): F[Unit]

  def hasPostsAfter(userId: UserId, lastPostId: PostId): F[Boolean]

  def hasPosts(userId: UserId): F[Boolean]

}
