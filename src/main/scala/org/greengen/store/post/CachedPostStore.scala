package org.greengen.store.post

import cats.effect.IO
import cats.implicits._
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.post.{Post, PostId, SearchPostType}
import org.greengen.core.user.UserId
import org.greengen.core._


object CachedPostStore {
  def withCache(store: PostStore[IO]): PostStore[IO] =
    new CachedPostStore(new InMemoryPostStore, store)
}

// Using an in-memory cache implementation to remember fetched posts
// Queries are trying to hit the cache first, and go to the persisted store in case of miss
private[post] class CachedPostStore(val cache: PostStore[IO],
                      val persistent: PostStore[IO]) extends PostStore[IO] {

  override def registerPost(post: Post): IO[Unit] = for {
    _ <- persistent.registerPost(post)
    _ <- cache.registerPost(post)
  } yield ()

  override def exists(postId: PostId): IO[Boolean] = for {
    exist <- cache.exists(postId)
    res <- if (exist) IO(true) else persistent.exists(postId)
  } yield res

  override def getPostById(postId: PostId): IO[Option[Post]] = for {
    maybePost <- cache.getPostById(postId)
    result    <- maybePost.fold(persistent.getPostById(postId))(p => IO(Some(p)))
    _         <- (maybePost, result) match {
        case (None, Some(p)) => cache.registerPost(p)
        case _               => IO.unit
      }
  } yield result


  override def getPostByIds(postIds: List[PostId]): IO[List[Post]] = for {
    cachedPosts  <- cache.getPostByIds(postIds)
    foundIds = cachedPosts.map(_.id).toSet
    notFound = postIds.filterNot(foundIds(_))
    notYetCached <- persistent.getPostByIds(notFound)
    result       <- IO(cachedPosts ++ notYetCached)
    // Bringing non-cached posts into memory cache
    _            <- notYetCached match {
      case xs if xs.nonEmpty => xs.map { post => cache.registerPost(post) }.sequence
      case _                 => IO.unit
    }
  } yield result

  override def getByAuthor(author: UserId, postType: SearchPostType, page: Page): IO[List[PostId]] =
    persistent.getByAuthor(author, postType, page)

  override def getByChallengeId(challengeId: ChallengeId): IO[Option[PostId]] =
    IOUtils.orElse(cache.getByChallengeId(challengeId), persistent.getByChallengeId(challengeId))

  override def getByEventId(eventId: EventId): IO[Option[PostId]] =
    IOUtils.orElse(cache.getByEventId(eventId), persistent.getByEventId(eventId))

  override def getByHashtags(tags: Set[Hashtag], postType: SearchPostType, page: Page): IO[List[PostId]] =
    persistent.getByHashtags(tags, postType, page)

  override def trendByHashtags(n: Int): IO[List[(Int, Hashtag)]] =
    persistent.trendByHashtags(n)

  override def flagPost(flaggedBy: UserId, post: PostId, reason: Reason, timestamp: UTCTimestamp): IO[Unit] =
    for {
      _ <- persistent.flagPost(flaggedBy, post, reason, timestamp)
      _ <- cache.flagPost(flaggedBy, post, reason, timestamp)
    } yield ()

  override def isPostFlagged(postId: PostId): IO[Boolean] = for {
    flagged <- cache.isPostFlagged(postId)
    res <- if (flagged) IO(true) else persistent.isPostFlagged(postId)
  } yield res

  override def randomPosts(n: Int): IO[List[PostId]] =
    persistent.randomPosts(n)
}
