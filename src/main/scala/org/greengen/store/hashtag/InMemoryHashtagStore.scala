package org.greengen.store.hashtag

import cats.effect.IO
import org.greengen.core.Hashtag
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

class InMemoryHashtagStore extends HashtagStore[IO] {

  private[this] val byHashtags = new TrieMap[Hashtag, Set[UserId]]()
  private[this] val byUsers = new TrieMap[UserId, Set[Hashtag]]()

  override def getFollowers(ht: Hashtag): IO[Set[UserId]] =
    IO(byHashtags.getOrElse(ht, Set()))

  override def countFollowers(ht: Hashtag): IO[Int] =
    IO(byHashtags.getOrElse(ht, Set()).size)

  override def addHashtagFollower(userId: UserId, ht: Hashtag): IO[Unit] =
    updateMappings(ht, userId)

  override def removeHashtagFollower(userId: UserId, ht: Hashtag): IO[Unit] =
    removeMappings(ht, userId)

  override def hashtagsfollowedByUser(userId: UserId): IO[Set[Hashtag]] =
    IO(byUsers.getOrElse(userId, Set()))

  override def trendByFollowers(n: Int): IO[List[(Int, Hashtag)]] = IO {
    byHashtags
      .map { case (ht,followers) => (followers.size, ht) }
      .toList
      .sortBy(_._1)
      .take(math.max(0,n))
  }

  // Helpers

  // Helpers

  private[this] def updateMappings(ht: Hashtag, user: UserId): IO[Unit] = IO {
    byHashtags.updateWith(ht) {
      case Some(users) => Some(users + user)
      case None => Some(Set(user))
    }
    byUsers.updateWith(user) {
      case Some(hashtags) => Some(hashtags + ht)
      case None => Some(Set(ht))
    }
  }

  private[this] def removeMappings(ht: Hashtag, user: UserId): IO[Unit] = IO {
    byHashtags.updateWith(ht) {
      case Some(users) => Some(users - user)
      case None => None
    }
    byUsers.updateWith(user) {
      case Some(hashtags) => Some(hashtags - ht)
      case None => None
    }
  }

}
