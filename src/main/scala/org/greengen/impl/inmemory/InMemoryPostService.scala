package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.feed.FeedService
import org.greengen.core.follower.FollowerService
import org.greengen.core.notification.NotificationService
import org.greengen.core.post.{Post, PostId, PostService, RePost}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.wall.WallService
import org.greengen.core._

import scala.collection.concurrent.TrieMap

class InMemoryPostService(clock: Clock,
                          userService: UserService[IO],
                          wallService: WallService[IO])
  extends PostService[IO] {

  private[this] val posts = new TrieMap[PostId, Post]()
  private[this] val flagged = new TrieMap[PostId, List[(UserId, Reason, UTCTimestamp)]]
  private[this] val hashtags = new TrieMap[Hashtag, Set[PostId]]()
  private[this] val authors = new TrieMap[UserId, Set[PostId]]()

  override def post(post: Post): IO[PostId] = for {
    _ <- checkUser(post.author)
    _ <- IO(indexById(post))
    _ <- IO(indexByHashtags(post))
    _ <- IO(indexByAuthor(post))
    _ <- wallService.addToWall(post.author, post.id)
  } yield post.id

  override def repost(user: UserId, postId: PostId): IO[PostId] = for {
    _ <- checkUser(user)
    postOpt <- byId(postId)
    Some(originalPost) = postOpt // FIXME
    _ <- checkUser(originalPost.author)
    repostId = PostId.newId
    _ <- post(RePost(repostId, user, postId, clock.now(), originalPost.hashtags))
  } yield repostId

  override def flag(flaggedBy: UserId, post: PostId, reason: Reason): IO[Unit] = for {
    _ <- checkUser(flaggedBy)
    _ <- IO(flagPost(flaggedBy, post, reason, clock.now()))
  } yield ()

  override def isFlagged(post: PostId): IO[Boolean] =
    IO(flagged.contains(post))

  override def byId(post: PostId): IO[Option[Post]] =
    IO(posts.get(post))

  override def byAuthor(userId: UserId): IO[Set[PostId]] =
    IO(authors.getOrElse(userId, Set()))

  override def byHashtags(tags: Set[Hashtag]): IO[Set[PostId]] = IO {
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

  // Helpers

  private[this] def flagPost(flaggedBy: UserId, post: PostId, reason: Reason, timestamp: UTCTimestamp): Unit = {
    flagged.updateWith(post) {
      case Some(reports) => Some((flaggedBy,reason, timestamp) :: reports)
      case None => Some(List((flaggedBy, reason, timestamp)))
    }
  }

  private[this] def indexById(post: Post): Unit =
    posts.put(post.id, post)

  private[this] def indexByHashtags(post: Post): Unit =
    post.hashtags.foreach { ht =>
      hashtags.updateWith(ht) {
        case Some(ids) => Some(ids + post.id)
        case None => Some(Set(post.id))
      }
    }

  private[this] def indexByAuthor(post: Post): Unit =
    authors.updateWith(post.author) {
      case Some(ids) => Some(ids + post.id)
      case None => Some(Set(post.id))
    }


  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkPostExists(post: PostId, posts: List[PostId]) =
    IOUtils.check(posts.contains(post), s"Post $post doesn't exist")

}
