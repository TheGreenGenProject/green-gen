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
  } yield Feed(userId, content)

  override def addToFeed(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- feedStore.addPost(userId, postId)
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
    last <- feedStore.mostRecentPost(userId)
  } yield last.exists(_!=lastPostId)

  override def hasPosts(userId: UserId): IO[Boolean] = for {
    _   <- checkUser(userId)
    res <- feedStore.hasPosts(userId)
  } yield res


  // Helpers

  private[this] def getFeedPage(userId: UserId, page: Page): IO[List[PostId]] =
    feedStore.getByUserId(userId, page)

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}

