package org.greengen.impl.inmemory

import cats.effect.IO
import cats.implicits._
import org.greengen.core.feed.{Feed, FeedService}
import org.greengen.core.follower.FollowerService
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{IOUtils, Page, PagedResult}

import scala.collection.concurrent.TrieMap

class InMemoryFeedService(userService: UserService[IO],
                          followerService: FollowerService[IO])
  extends FeedService[IO]{

  private[this] val feeds = new TrieMap[UserId, IndexedSeq[PostId]]()


  override def feed(userId: UserId, page: Page): IO[Feed] = for {
    _ <- checkUser(userId)
    content <- IO(getFeedPage(userId, page))
  } yield Feed(userId, content.toList)

  override def addToFeed(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- IO(addPostToFeed(userId, postId))
  } yield ()

  override def addToFollowersFeed(userId: UserId, postId: PostId): IO[Unit] = for {
    _         <- checkUser(userId)
    followers <- followerService.followers(userId)
    _         <- followers.toList.map(addToFeed(_, postId)).sequence
  } yield ()

  override def hasPostsAfter(userId: UserId, lastPostId: PostId): IO[Boolean] = for {
    _   <- checkUser(userId)
    res <- IO(feeds.getOrElse(userId, IndexedSeq())
              .headOption
              .exists( _ != lastPostId))
  } yield res

  override def hasPosts(userId: UserId): IO[Boolean] = for {
    _   <- checkUser(userId)
    res <- IO(feeds.getOrElse(userId, IndexedSeq()).nonEmpty)
  } yield res

  // Helpers

  private[this] def getFeedPage(userId: UserId, page: Page): IndexedSeq[PostId] =
    PagedResult.page(feeds.getOrElse(userId, IndexedSeq()), page)

  private[this] def addPostToFeed(userId: UserId, postId: PostId): Unit =
    feeds.updateWith(userId) {
      case Some(posts) => Some(postId +: posts)
      case None => Some(IndexedSeq(postId))
    }

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
