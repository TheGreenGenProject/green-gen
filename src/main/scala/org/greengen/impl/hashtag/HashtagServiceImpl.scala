package org.greengen.impl.hashtag

import cats.effect.IO
import org.greengen.core.hashtag.HashtagService
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Hashtag, IOUtils}
import org.greengen.store.hashtag.HashtagStore


class HashtagServiceImpl(hashtagStore: HashtagStore[IO])
                        (userService: UserService[IO]) extends HashtagService[IO] {


  override def startFollowing(src: UserId, hashtag: Hashtag): IO[Unit] = for {
    _     <- checkUser(src)
    _     <- hashtagStore.addHashtagFollower(src, hashtag)
  } yield ()

  override def stopFollowing(src: UserId, hashtag: Hashtag): IO[Unit] = for {
    _     <- checkUser(src)
    _     <- hashtagStore.removeHashtagFollower(src, hashtag)
  } yield ()

  override def followers(hashtag: Hashtag): IO[Set[UserId]] =
    hashtagStore.getFollowers(hashtag)

  override def countFollowers(hashtag: Hashtag): IO[Int] =
    hashtagStore.countFollowers(hashtag)

  override def hashtagsfollowedBy(userId: UserId): IO[Set[Hashtag]] =
    hashtagStore.hashtagsfollowedByUser(userId)

  override def trendByFollowers(n: Int): IO[List[(Int, Hashtag)]] =
    hashtagStore.trendByFollowers(n)


  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()
}
