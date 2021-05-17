package org.greengen.impl.feed

import cats.effect.IO
import cats.implicits._
import org.greengen.core.feed.{Feed, FeedService}
import org.greengen.core.follower.FollowerService
import org.greengen.core.hashtag.HashtagService
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Hashtag, IOUtils, Page, PagedResult}
import org.greengen.store.feed.FeedStore


class FeedServiceImpl(feedStore: FeedStore[IO])
                     (userService: UserService[IO],
                      followerService: FollowerService[IO],
                      hashtagService: HashtagService[IO])
  extends FeedService[IO] {


  override def feed(userId: UserId, page: Page): IO[Feed] = for {
    _       <- checkUser(userId)
    content <- getFeedPage(userId, page)
  } yield Feed(userId, content.toList)

  override def addToFeed(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- addPostToFeed(userId, postId)
  } yield ()

  override def addToFollowersFeed(userId: UserId, postId: PostId): IO[Unit] = for {
    _         <- checkUser(userId)
    followers <- followerService.followers(userId)
    _         <- followers.toList.map(addToFeed(_, postId)).sequence
  } yield ()

  override def addToHashtagFollowersFeed(hashtags: Set[Hashtag], postId: PostId): IO[Unit] = for {
    followers <- hashtags.toList.map(hashtagService.followers(_).map(_.toList)).sequence
    _         <- followers.flatten.map(addToFeed(_, postId)).sequence
  } yield ()

  override def hasPostsAfter(userId: UserId, lastPostId: PostId): IO[Boolean] = for {
    _     <- checkUser(userId)
    posts <- feedStore.getByUserIdOrElse(userId, IndexedSeq())
    res   <- IO(posts.headOption.exists( _ != lastPostId))
  } yield res

  override def hasPosts(userId: UserId): IO[Boolean] = for {
    _   <- checkUser(userId)
    posts <- feedStore.getByUserIdOrElse(userId, IndexedSeq())
  } yield posts.nonEmpty


  // Helpers

  private[this] def getFeedPage(userId: UserId, page: Page): IO[IndexedSeq[PostId]] = for {
    posts <- feedStore.getByUserIdOrElse(userId, IndexedSeq())
    res   <- IO(PagedResult.page(posts, page))
  } yield res

  private[this] def addPostToFeed(userId: UserId, postId: PostId): IO[Unit] =
    feedStore.updateWith(userId) {
      case Some(posts) => Some(postId +: posts)
      case None => Some(IndexedSeq(postId))
    }

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}

