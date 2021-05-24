package org.greengen.store.hashtag

import org.greengen.core.Hashtag
import org.greengen.core.user.UserId
import org.greengen.store.Store

trait HashtagStore[F[_]] extends Store[F] {

  def getFollowers(ht: Hashtag): F[Set[UserId]]

  def countFollowers(ht: Hashtag): F[Long]

  def addHashtagFollower(userId: UserId, ht: Hashtag): F[Unit]

  def removeHashtagFollower(userId: UserId, ht: Hashtag): F[Unit]

  def hashtagsfollowedByUser(userId: UserId): F[Set[Hashtag]]

  def trendByFollowers(n: Int): F[List[(Int, Hashtag)]]

}
