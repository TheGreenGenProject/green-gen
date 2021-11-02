package org.greengen.core.post

import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.{Hashtag, Page, Reason}
import org.greengen.core.user.UserId

trait PostService[F[_]] {

  def post(post: Post): F[PostId]

  def repost(user: UserId, post: PostId): F[PostId]

  def flag(flaggedBy: UserId, post: PostId, reason: Reason): F[Unit]

  def isFlagged(post: PostId): F[Boolean]

  def initialFeed(userId: UserId, n: Int): F[Unit]

  // Search capabilities

  def byId(post: PostId): F[Option[Post]]

  def byIds(posts: List[PostId]): F[List[Post]]

  def byContent(challenge: ChallengeId): F[Option[PostId]]

  def byContent(event: EventId): F[Option[PostId]]

  def byAuthor(userId: UserId, postType: SearchPostType, page: Page): F[List[PostId]]

  def byHashtags(hashtags: Set[Hashtag], postType: SearchPostType, page: Page): F[List[PostId]]

  def trendByHashtags(n: Int): F[List[(Int, Hashtag)]]

}
