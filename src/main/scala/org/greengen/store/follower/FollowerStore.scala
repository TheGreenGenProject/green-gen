package org.greengen.store.follower

import org.greengen.core.user.UserId
import org.greengen.store.Store

trait FollowerStore[F[_]] extends Store[F] {

  def getFollowersByUser(userId: UserId): F[Set[UserId]]

  def getFollowingByUser(userId: UserId): F[Set[UserId]]

  def startFollowing(src: UserId, dst: UserId): F[Unit]

  def stopFollowing(src: UserId, dst: UserId): F[Unit]

}
