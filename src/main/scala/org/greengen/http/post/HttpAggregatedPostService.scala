package org.greengen.http.post

import cats.data.OptionT
import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.challenge.{Challenge, _}
import org.greengen.core.conversation.ConversationService
import org.greengen.core.event.{Event, EventService}
import org.greengen.core.follower.FollowerService
import org.greengen.core.like.{Like, LikeService}
import org.greengen.core.partnership.{Partner, PartnershipService}
import org.greengen.core.pin.PinService
import org.greengen.core.poll.{Poll, PollService, PollStats}
import org.greengen.core.post._
import org.greengen.core.tip.{Tip, TipService}
import org.greengen.core.user.{Profile, UserId, UserService}
import org.greengen.core.{Clock, _}
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._



//fetchAndCacheAll: Cache -> UserInfo -> List Post -> Task Http.Error Cache
//fetchAndCacheAll cache user posts = cache
//|> Task.succeed
//|> Task.andThen (\cache1 -> fetchAndCacheAllUsersFromPosts cache1 user posts)
//|> Task.andThen (\cache2 -> fetchAndCacheAllPosts cache2 user posts)
//|> Task.andThen (\cache3 -> fetchAndCacheLikes cache3 user posts)
//|> Task.andThen (\cache4 -> fetchAndCachePins cache4 user posts)
//|> Task.andThen (\cache5 -> fetchAndCacheFollowingUsers cache5 user)
//|> Task.andThen (\cache6 -> fetchAndCacheFollowers cache6 user)
//|> Task.andThen (\cache7 -> fetchAndCacheFollowingHashtags cache7 user)
//|> Task.andThen (\cache8 -> fetchAndCacheAllMessageCounts cache8 user posts)
//|> Task.andThen (\cache9 -> fetchAndCacheAllPostPartnership cache9 user posts)


// Aggregate all information required to get a list of post in one query
object HttpAggregatedPostService {

  def routes(clock: Clock,
             userService: UserService[IO],
             postService: PostService[IO],
             challengeService: ChallengeService[IO],
             pollService: PollService[IO],
             eventService: EventService[IO],
             tipService: TipService[IO],
             pinService: PinService[IO],
             likeService: LikeService[IO],
             followerService: FollowerService[IO],
             partnershipService: PartnershipService[IO],
             conversationService: ConversationService[IO]
  ) = {
    val info = getAggregatedPostInfo(clock,
      userService,
      postService,
      challengeService,
      pollService,
      eventService,
      tipService,
      pinService,
      likeService,
      followerService,
      partnershipService,
      conversationService) _

    AuthedRoutes.of[UserId, IO] {
      // GET
      case GET -> Root / "post" / "by-ids" :? PostIdsQueryParamMatcher(postIds) as userId =>
        val all = for {
          posts   <- postIds.map(info(_, userId))
            .sequence.map(_.flatten)
          // Extracting post info from reposts
          reposts <- posts
            .collect { case pa if pa.repost.isDefined => info(pa.repost.get.repost.originalId, userId) }
            .sequence.map(_.flatten)
        } yield posts ++ reposts
        all.flatMap(r => Ok(r.asJson))
    }
  }


  // Aggregated post information

  case class AggregatedPostInfo(
    postId: PostId,
    post: Post,
    user: Profile,
    pinned: Boolean,
    partner: Option[Partner],
    tip: Option[TipInfo],
    freeText: Option[FreeTextInfo],
    challenge: Option[ChallengeInfo],
    event: Option[EventInfo],
    poll: Option[PollInfo],
    repost: Option[RepostInfo],
    likes: Like,
    messageCount: Long)

  case class EventInfo(
    event: Event,
    cancelled: Boolean,
    participationStatus: Boolean,
    participationRequestStatus: Boolean,
    participationCount: Long)
  case class ChallengeInfo(
    challenge: Challenge,
    status: ChallengeStatus,
    statusOutcome: ChallengeOutcomeStatus,
    statistics: ChallengeStatistics)
  case class PollInfo(
    poll: Poll,
    answered: Boolean,
    statistics: PollStats)
  case class TipInfo(tip: Tip)
  case class RepostInfo(repost: RePost)
  case class FreeTextInfo(post: FreeTextPost)


  private[this] def getAggregatedPostInfo(
    clock: Clock,
    userService: UserService[IO],
    postService: PostService[IO],
    challengeService: ChallengeService[IO],
    pollService: PollService[IO],
    eventService: EventService[IO],
    tipService: TipService[IO],
    pinService: PinService[IO],
    likeService: LikeService[IO],
    followerService: FollowerService[IO],
    partnershipService: PartnershipService[IO],
    conversationService: ConversationService[IO])
    (postId: PostId, userId: UserId): IO[Option[AggregatedPostInfo]] = {
    val res = for {
      post          <- IOUtils.defined(postService.byId(postId),
        s"Couldn't find post $postId")
      user          <- IOUtils.defined(userService.byId(post.author),
        s"Cannot find author ${post.author} for post $postId")
      partnerId     <- partnershipService.partnerFor(postId)
      partner       <- partnerId.fold(IO[Option[Partner]](None))(partnershipService.partnerById(_))
      profile = user._2
      isPinned      <- pinService.isPinned(userId, postId)
      likes         <- likeService.countLikes(postId)
      eventInfo     <- getEventInfo(eventService)(post, userId)
      challengeInfo <- getChallengeInfo(challengeService)(post, userId)
      pollInfo      <- getPollInfo(pollService)(post, userId)
      tipInfo       <- getTipInfo(tipService)(post, userId)
      freeTextInfo  <- getFreeTextInfo(postService)(post, userId)
      repostInfo    <- getRepostInfo(postService)(post, userId)
      messageCount  <- conversationService.countMessages(postId)
    } yield Some(AggregatedPostInfo(
      postId = postId,
      post = post,
      user = profile,
      partner = partner,
      pinned = isPinned,
      tip = tipInfo,
      freeText = freeTextInfo,
      challenge = challengeInfo,
      event = eventInfo,
      poll = pollInfo,
      repost = repostInfo,
      likes = likes,
      messageCount = messageCount))
    res
  }

  private[this] def getEventInfo(eventService: EventService[IO])
                                (post: Post, userId: UserId): IO[Option[EventInfo]] =
    (for {
        eventPost          <- OptionT.fromOption[IO](Post.asEvent(post))
        event              <- OptionT(eventService.byId(eventPost.event))
        cancelled          <- OptionT(eventService.isCancelled(event.id).map(Option(_)))
        isParticipating    <- OptionT(eventService.isParticipating(event.id, userId).map(Option(_)))
        isRequested        <- OptionT(eventService.isParticipationRequested(event.id, userId).map(Option(_)))
        participationCount <- OptionT(eventService.participantCount(event.id).map(Option(_)))
      } yield EventInfo(
        event = event,
        cancelled = cancelled,
        participationStatus = isParticipating,
        participationRequestStatus = isRequested,
        participationCount = participationCount)).value

  private[this] def getChallengeInfo(challengeService: ChallengeService[IO])
                                    (post: Post, userId: UserId): IO[Option[ChallengeInfo]] =
    (for {
      challengePost <- OptionT.fromOption[IO](Post.asChallenge(post))
      challenge     <- OptionT(challengeService.byId(challengePost.challenge))
      status        <- OptionT(challengeService.status(challenge.id).map(Option(_)))
      statusOutcome <- OptionT(challengeService.status(userId, challenge.id).map(Option(_)))
      statistics    <- OptionT(challengeService.statistics(challenge.id).map(Option(_)))
    } yield ChallengeInfo(
      challenge = challenge,
      status = status,
      statusOutcome = statusOutcome,
      statistics = statistics)).value

  private[this] def getTipInfo(tipService: TipService[IO])
                               (post: Post, userId: UserId): IO[Option[TipInfo]] =
    (for {
      tipPost   <- OptionT.fromOption[IO](Post.asTip(post))
      tip       <- OptionT(tipService.byId(tipPost.tip))
    } yield TipInfo(tip)).value

  private[this] def getPollInfo(pollService: PollService[IO])
                               (post: Post, userId: UserId): IO[Option[PollInfo]] =
    (for {
      pollPost   <- OptionT.fromOption[IO](Post.asPoll(post))
      poll       <- OptionT(pollService.byId(pollPost.poll))
      answered   <- OptionT(pollService.hasResponded(poll.id, userId).map(Option(_)))
      statistics <-OptionT(pollService.statisics(poll.id).map(Option(_)))
    } yield PollInfo(
      poll = poll,
      answered = answered,
      statistics = statistics
    )).value

  private[this] def getFreeTextInfo(postService: PostService[IO])
                                   (post: Post, userId: UserId): IO[Option[FreeTextInfo]] =
    (for {
      freeTextPost <- OptionT.fromOption[IO](Post.asFreeText(post))
    } yield FreeTextInfo(freeTextPost)).value

  private[this] def getRepostInfo(postService: PostService[IO])
                                 (post: Post, userId: UserId): IO[Option[RepostInfo]] =
    (for {
      repost <- OptionT.fromOption[IO](Post.asRepost(post))
    } yield RepostInfo(repost)).value


}
