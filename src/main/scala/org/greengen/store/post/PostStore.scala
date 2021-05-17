package org.greengen.store.post

import org.greengen.core.{Hashtag, Reason, UTCTimestamp}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.post.{Post, PostId}
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait PostStore[F[_]] extends Store[F] {

  def registerPost(post: Post): F[Unit]

  def exists(postId: PostId): F[Boolean]

  def getPostById(postId: PostId): F[Option[Post]]

  def getByAuthor(author: UserId): F[Set[PostId]]

  def getByChallengeId(challengeId: ChallengeId): F[Option[PostId]]

  def getByHashtags(tags: Set[Hashtag]): F[Set[PostId]]

  def trendByHashtags(n: Int): F[List[(Int, Hashtag)]]

  def flagPost(flaggedBy: UserId, post: PostId, reason: Reason, timestamp: UTCTimestamp): F[Unit]

  def isPostFlagged(post: PostId): F[Boolean]

}
