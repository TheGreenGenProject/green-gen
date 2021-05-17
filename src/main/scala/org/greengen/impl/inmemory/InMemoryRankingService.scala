package org.greengen.impl.inmemory

import cats.effect.IO
import cats.implicits._
import org.greengen.core.IOUtils
import org.greengen.core.event.EventService
import org.greengen.core.follower.FollowerService
import org.greengen.core.like.LikeService
import org.greengen.core.post.PostService
import org.greengen.core.ranking.{Rank, RankingService, ScoreBreakdown}
import org.greengen.core.user.{UserId, UserService}


@deprecated
class InMemoryRankingService(userService: UserService[IO],
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
    maybeUser      <- userService.profile(userId)
    profile        <- IOUtils.from(maybeUser, s"Unknown user id $userId")
    likesReceived  <- countLikesReceived(userId)
    followingCount <- followerService.countFollowing(userId)
    followerCount  <- followerService.countFollowers(userId)
    postCount      <- postService.byAuthor(userId).map(_.size)
    eventsOrganized <- eventService.byOwnership(userId).map(_.size)
    eventsAttended  <- eventService.byParticipation(userId).map(_.size)
  } yield ScoreBreakdown.compute(profile, likesReceived, followingCount,
      followerCount, postCount, eventsOrganized, eventsAttended)


  // Helpers

  private[this] def countLikesReceived(userId: UserId): IO[Long] = for {
    posts        <- postService.byAuthor(userId)
    likesPerPost <- posts.toList.map(likeService.countLikes(_)).sequence
    totalLikes   <- IO(likesPerPost.map(_.value).sum)
  } yield totalLikes
}
