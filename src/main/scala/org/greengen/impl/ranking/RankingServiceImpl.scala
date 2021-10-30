package org.greengen.impl.ranking

import cats.effect.IO
import cats.implicits._
import org.greengen.core.{IOUtils, Page, PagedResult}
import org.greengen.core.event.EventService
import org.greengen.core.follower.FollowerService
import org.greengen.core.like.LikeService
import org.greengen.core.post.{AllPosts, PostId, PostService}
import org.greengen.core.ranking.{Rank, RankingService, ScoreBreakdown}
import org.greengen.core.user.{UserId, UserService}


class RankingServiceImpl(userService: UserService[IO],
                         likeService: LikeService[IO],
                         followerService: FollowerService[IO],
                         postService: PostService[IO],
                         eventService: EventService[IO]) extends RankingService[IO] {

  override def rank(userId: UserId): IO[Rank] = for {
    breakdown <- score(userId)
    score     <- IO(breakdown.asScore)
    rank      <- IO(Rank.fromScore(score))
  } yield rank

  override def score(userId: UserId): IO[ScoreBreakdown] = for {
    maybeUser       <- userService.profile(userId)
    profile         <- IOUtils.from(maybeUser, s"Unknown user id $userId")
    postIds         <- postService.byAuthor(userId, AllPosts, Page.All)
    likesReceived   <- countLikesReceived(postIds)
    followingCount  <- followerService.countFollowing(userId)
    followerCount   <- followerService.countFollowers(userId)
    postCount       <- IO(postIds.size)
    eventsOrganized <- eventService.byOwnership(userId, Page.All).map(_.size)
    eventsAttended  <- eventService.byParticipation(userId, Page.All).map(_.size)
  } yield ScoreBreakdown.compute(profile, likesReceived, followingCount,
      followerCount, postCount, eventsOrganized, eventsAttended)


  // Helpers

  private[this] def countLikesReceived(posts: List[PostId]): IO[Long] = for {
    likesPerPost <- posts.map(likeService.countLikes(_)).sequence
    totalLikes   <- IO(likesPerPost.map(_.value).sum)
  } yield totalLikes
}
