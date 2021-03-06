package org.greengen.store.post

import cats.effect.IO
import org.greengen.core.{Hashtag, Page, PagedResult, Reason, UTCTimestamp}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.post.{AllPosts, ChallengePost, ChallengePosts, EventPost, EventPosts, FreeTextPost, FreeTextPosts, PollPost, PollPosts, Post, PostId, RePost, SearchPostType, TipPost, TipPosts}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap



class InMemoryPostStore extends PostStore[IO] {

  private[this] val posts = new TrieMap[PostId, Post]()
  private[this] val flagged = new TrieMap[PostId, List[(UserId, Reason, UTCTimestamp)]]
  private[this] val hashtags = new TrieMap[Hashtag, Set[PostId]]()
  private[this] val authors = new TrieMap[UserId, Set[PostId]]()
  private[this] val challenges = new TrieMap[ChallengeId, PostId]()
  private[this] val events = new TrieMap[EventId, PostId]()


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

  override def getPostByIds(postIds: List[PostId]): IO[List[Post]] =
    IO(postIds.flatMap(posts.get(_)))

  override def getByAuthor(author: UserId, postType: SearchPostType, page: Page): IO[List[PostId]] =
    for {
      all <- IO(authors.getOrElse(author, Set()))
      res <- applySearch(all, postType, page)
    } yield res

  override def getByChallengeId(challengeId: ChallengeId): IO[Option[PostId]] =
    IO(challenges.get(challengeId))

  override def getByEventId(eventId: EventId): IO[Option[PostId]] =
    IO(events.get(eventId))

  override def getByHashtags(tags: Set[Hashtag], postType: SearchPostType, page: Page): IO[List[PostId]] =
    for {
      all <- IO { tags
        .map(tag => hashtags.getOrElse(tag, Set[PostId]()))
        .reduce((a, b) => a.intersect(b)) }
      res <- applySearch(all, postType, page)
    } yield res

  override def trendByHashtags(n: Int): IO[List[(Int, Hashtag)]] = IO {
    hashtags.toList
      .map { case (ht, posts) => (posts.size, ht) }
      .sortBy(- _._1)
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

  def randomPosts(n: Int): IO[List[PostId]] =
    IO(posts.values.map(_.id).take(n).toList)

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
      case ep: EventPost     => indexByEventId(ep.event, ep)
      case _ =>
    }
  }

  private[this] def indexByChallengeId(challengeId: ChallengeId, post: Post): Unit =
    challenges.put(challengeId, post.id)

  private[this] def indexByEventId(eventId: EventId, post: Post): Unit =
    events.put(eventId, post.id)

  // Search helpers

  private[this] def applySearch(postIds: Set[PostId], postType: SearchPostType, page: Page): IO[List[PostId]] =
    for {
      filtered <- IO(postIds.filter(matchPostType(_, postType)).toList)
      sorted   <- IO(filtered.sortBy(id => posts.get(id).map(- _.created.value).getOrElse(0L))) // order by desc
      paged    <- IO(PagedResult.page(sorted, page))
    } yield paged

  private[this] def matchPostType(postId: PostId, postType: SearchPostType): Boolean =
    posts.get(postId).exists(matchPostType(_, postType))

  private[this] def matchPostType(post: Post, postType: SearchPostType): Boolean =
    (post, postType) match {
      case (_: RePost, _)                     => false // ignoring all reposts
      case (_, AllPosts)                      => true
      case (_: TipPost, TipPosts)             => true
      case (_: ChallengePost, ChallengePosts) => true
      case (_: PollPost, PollPosts)           => true
      case (_: EventPost, EventPosts)         => true
      case (_: FreeTextPost, FreeTextPosts)   => true
      case _                                  => false
    }

}
