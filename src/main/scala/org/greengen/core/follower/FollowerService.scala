package org.greengen.core.follower

import org.greengen.core.user.UserId

trait FollowerService[F[_]] {

  def startFollowing(src: UserId, dst: UserId): F[Unit]

  def stopFollowing(src: UserId, dst: UserId): F[Unit]

  def followers(id: UserId): F[Set[UserId]]

  def countFollowers(id: UserId): F[Int]

  def following(id: UserId): F[Set[UserId]]

  def countFollowing(id: UserId): F[Int]

}
