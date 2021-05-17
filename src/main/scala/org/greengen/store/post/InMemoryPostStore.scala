package org.greengen.store.post

import cats.effect.IO
import org.greengen.core.{Hashtag, Reason, UTCTimestamp}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.post.{ChallengePost, Post, PostId}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap



class InMemoryPostStore extends PostStore[IO] {

  private[this] val posts = new TrieMap[PostId, Post]()
  private[this] val flagged = new TrieMap[PostId, List[(UserId, Reason, UTCTimestamp)]]
  private[this] val hashtags = new TrieMap[Hashtag, Set[PostId]]()
  private[this] val authors = new TrieMap[UserId, Set[PostId]]()
  private[this] val challenges = new TrieMap[ChallengeId, PostId]()


  override def registerPost(post: Post): IO[Unit] = for {
    _ <- indexById(post)
    _ <- indexByHashtags(post)
    _ <- indexByAuthor(post)
    _ <- indexByContent(post)
  } yield ()

  override def exists(postId: PostId): IO[Boolean] =
    IO(posts.contains(postId))

  override def getPostById(postId: PostId): IO[Option[Post]] =
    IO(posts.get(postId))

  override def getByAuthor(author: UserId): IO[Set[PostId]] =
    IO(authors.getOrElse(author, Set()))

  override def getByChallengeId(challengeId: ChallengeId): IO[Option[PostId]] =
    IO(challenges.get(challengeId))

  override def getByHashtags(tags: Set[Hashtag]): IO[Set[PostId]] = IO {
    tags
      .map(tag => hashtags.getOrElse(tag, Set[PostId]()))
      .reduce((a, b) => a.intersect(b))
  }

  override def trendByHashtags(n: Int): IO[List[(Int, Hashtag)]] = IO {
    hashtags.toList
      .map { case (ht, posts) => (posts.size, ht) }
      .sortBy(_._1)
      .take(n)
  }

  override def flagPost(flaggedBy: UserId, post: PostId, reason: Reason, timestamp: UTCTimestamp): IO[Unit] = IO {
    flagged.updateWith(post) {
      case Some(reports) => Some((flaggedBy,reason, timestamp) :: reports)
      case None => Some(List((flaggedBy, reason, timestamp)))
    }
  }

  override def isPostFlagged(post: PostId): IO[Boolean] =
    IO(flagged.contains(post))


  // Helpers

  private[this] def indexById(post: Post): IO[Unit] =
    IO(posts.put(post.id, post))

  private[this] def indexByHashtags(post: Post): IO[Unit] = IO {
    post.hashtags.foreach { ht =>
      hashtags.updateWith(ht) {
        case Some(ids) => Some(ids + post.id)
        case None => Some(Set(post.id))
      }
    }
  }

  private[this] def indexByAuthor(post: Post): IO[Unit] = IO {
    authors.updateWith(post.author) {
      case Some(ids) => Some(ids + post.id)
      case None => Some(Set(post.id))
    }
  }

  private[this] def indexByContent(post: Post): IO[Unit] = IO {
    post match {
      case cp: ChallengePost => indexByChallengeId(cp.challenge, cp)
      case _ =>
    }
  }

  private[this] def indexByChallengeId(challengeId: ChallengeId, post: Post): Unit =
    challenges.put(challengeId, post.id)

}
