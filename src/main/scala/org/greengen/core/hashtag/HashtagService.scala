package org.greengen.core.hashtag

import org.greengen.core.Hashtag
import org.greengen.core.user.UserId

trait HashtagService[F[_]] {

  def startFollowing(src: UserId, hashtag: Hashtag): F[Unit]

  def stopFollowing(src: UserId, hashtag: Hashtag): F[Unit]

  def followers(hashtag: Hashtag): F[Set[UserId]]

  def countFollowers(hashtag: Hashtag): F[Long]

  def hashtagsfollowedBy(userId: UserId): F[Set[Hashtag]]

  def trendByFollowers(n: Int): F[List[(Int, Hashtag)]]

}
