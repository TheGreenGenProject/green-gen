package org.greengen.impl.post

import cats.effect.IO
import cats.implicits._
import org.greengen.core._
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.feed.FeedService
import org.greengen.core.post._
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.wall.WallService
import org.greengen.store.post.PostStore

class PostServiceImpl(postStore: PostStore[IO])
                     (clock: Clock,
                      userService: UserService[IO],
                      wallService: WallService[IO],
                      feedService: FeedService[IO])
  extends PostService[IO] {

  override def post(post: Post): IO[PostId] = for {
    _ <- checkUser(post.author)
    _ <- postStore.registerPost(post)
    _ <- wallService.addToWall(post.author, post.id)
    _ <- feedService.addToFollowersFeed(post.author, post.id)
    _ <- feedService.addToHashtagFollowersFeed(post.hashtags, post.id)
  } yield post.id

  override def repost(user: UserId, postId: PostId): IO[PostId] = for {
    _            <- checkUser(user)
    postOpt      <- byId(postId)
    originalPost <- IOUtils.from(postOpt, s"Cannot find original post ${postId} for Repost")
    _            <- checkNotARepost(originalPost)
    _            <- checkUser(originalPost.author)
    repostId = PostId.newId
    _            <- post(RePost(repostId, user, postId, clock.now(), originalPost.hashtags))
  } yield repostId

  override def flag(flaggedBy: UserId, post: PostId, reason: Reason): IO[Unit] = for {
    _ <- checkUser(flaggedBy)
    _ <- postStore.flagPost(flaggedBy, post, reason, clock.now())
  } yield ()

  override def isFlagged(post: PostId): IO[Boolean] =
    postStore.isPostFlagged(post)

  override def initialFeed(userId: UserId, n: Int): IO[Unit] = for {
    posts <- postStore.randomPosts(n)
    _     <- posts.map(feedService.addToFeed(userId, _)).sequence
  } yield ()

  override def byId(post: PostId): IO[Option[Post]] =
    postStore.getPostById(post)

  override def byContent(challenge: ChallengeId): IO[Option[PostId]] =
    postStore.getByChallengeId(challenge)

  override def byContent(event: EventId): IO[Option[PostId]] =
    postStore.getByEventId(event)

  override def byAuthor(userId: UserId, postType: SearchPostType, page: Page): IO[List[PostId]] =
    postStore.getByAuthor(userId, postType, page)

  override def byHashtags(tags: Set[Hashtag], postType: SearchPostType, page: Page): IO[List[PostId]] =
    postStore.getByHashtags(tags, postType, page)

  override def trendByHashtags(n: Int): IO[List[(Int, Hashtag)]] =
    postStore.trendByHashtags(n)

  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkNotARepost(post: Post) = for {
    reposted  <- isRepost(post)
    _         <- IOUtils.check(!reposted, s"Can't repost a repost")
  } yield ()

  private[this] def isRepost(post: Post): IO[Boolean] =
    post match {
      case _:RePost => IO(true)
      case _        => IO(false)
    }
}
