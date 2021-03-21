package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.hashtag.HashtagService
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Hashtag, IOUtils}

import scala.collection.concurrent.TrieMap


class InMemoryHashtagService(userService: UserService[IO]) extends HashtagService[IO] {

  private[this] val byHashtags = new TrieMap[Hashtag, Set[UserId]]()
  private[this] val byUsers = new TrieMap[UserId, Set[Hashtag]]()


  override def startFollowing(src: UserId, hashtag: Hashtag): IO[Unit] = for {
    _     <- checkUser(src)
    _     <- IO(updateMappings(hashtag, src))
  } yield ()

  override def stopFollowing(src: UserId, hashtag: Hashtag): IO[Unit] = for {
    _     <- checkUser(src)
    _     <- IO(removeMappings(hashtag, src))
  } yield ()

  override def followers(hashtag: Hashtag): IO[Set[UserId]] = IO {
    byHashtags.getOrElse(hashtag, Set())
  }

  override def countFollowers(hashtag: Hashtag): IO[Int] = IO {
    byHashtags.getOrElse(hashtag, Set()).size
  }

  override def hashtagsfollowedBy(userId: UserId): IO[Set[Hashtag]] = IO {
    byUsers.getOrElse(userId, Set())
  }

  override def trendByFollowers(n: Int): IO[List[(Int, Hashtag)]] = IO {
    byHashtags
      .map { case (ht,followers) => (followers.size, ht) }
      .toList
      .sortBy(_._1)
      .take(math.max(0,n))
  }

  // Helpers

  private[this] def updateMappings(ht: Hashtag, user: UserId): Unit = {
    byHashtags.updateWith(ht) {
      case Some(users) => Some(users + user)
      case None => Some(Set(user))
    }
    byUsers.updateWith(user) {
      case Some(hashtags) => Some(hashtags + ht)
      case None => Some(Set(ht))
    }
  }

  private[this] def removeMappings(ht: Hashtag, user: UserId): Unit = {
    byHashtags.updateWith(ht) {
      case Some(users) => Some(users - user)
      case None => None
    }
    byUsers.updateWith(user) {
      case Some(hashtags) => Some(hashtags - ht)
      case None => None
    }
  }


  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()
}
