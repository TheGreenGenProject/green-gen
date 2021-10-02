package org.greengen.store.post

import org.greengen.core.{Hashtag, Page, Reason, UTCTimestamp}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.post.{Post, PostId, SearchPostType}
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait PostStore[F[_]] extends Store[F] {

  def registerPost(post: Post): F[Unit]

  def exists(postId: PostId): F[Boolean]

  def getPostById(postId: PostId): F[Option[Post]]

  def getByAuthor(author: UserId, postType: SearchPostType, page: Page): F[List[PostId]]

  def getByChallengeId(challengeId: ChallengeId): F[Option[PostId]]

  def getByEventId(eventId: EventId): F[Option[PostId]]

  def getByHashtags(tags: Set[Hashtag], postType: SearchPostType, page: Page): F[List[PostId]]

  def trendByHashtags(n: Int): F[List[(Int, Hashtag)]]

  def flagPost(flaggedBy: UserId, post: PostId, reason: Reason, timestamp: UTCTimestamp): F[Unit]

  def isPostFlagged(post: PostId): F[Boolean]

  def randomPosts(n: Int): F[List[PostId]]

}
