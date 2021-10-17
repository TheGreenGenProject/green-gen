package org.greengen.http

import cats.effect._
import io.circe.generic.auto._
import org.greengen.core.Coordinate.LatLong
import org.greengen.core.auth.Auth
import org.greengen.core.challenge._
import org.greengen.core.conversation.{ConversationId, Message, MessageId}
import org.greengen.core.event.{Event, EventId}
import org.greengen.core.feed.Feed
import org.greengen.core.like.Like
import org.greengen.core.notification._
import org.greengen.core.partnership.{Partner, PartnerId}
import org.greengen.core.pin.PinnedPost
import org.greengen.core.poll.{Poll, PollAnswer, PollId, PollOption, PollStats, PollStatsEntry}
import org.greengen.core.post.{Post, PostId}
import org.greengen.core.ranking.{Rank, ScoreBreakdown}
import org.greengen.core.tip.{Tip, TipId}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}
import org.greengen.core.wall.Wall
import org.greengen.core.{Country, Duration, Hashtag, Location, Schedule, Source, Token, UTCTimestamp, Url}
import org.greengen.http.post.HttpAggregatedPostService.{AggregatedPostInfo, ChallengeInfo, EventInfo, FreeTextInfo, PollInfo, RepostInfo, TipInfo}
import org.http4s.circe._

object JsonDecoder {

  implicit val StringDecoder = jsonOf[IO, String]
  implicit val StringOptionDecoder = jsonOf[IO, Option[String]]
  implicit val BooleanDecoder = jsonOf[IO, Boolean]
  implicit val BooleanOptionDecoder = jsonOf[IO, Option[Boolean]]
  implicit val IntDecoder = jsonOf[IO, Int]
  implicit val IntOptionDecoder = jsonOf[IO, Option[Int]]
  implicit val LongDecoder = jsonOf[IO, Long]
  implicit val LongOptionDecoder = jsonOf[IO, Option[Long]]

  // Auth service
  implicit val TokenDecoder = jsonOf[IO, Token]
  implicit val AuthDecoder = jsonOf[IO, Auth]
  // User service
  implicit val UserIdDecoder = jsonOf[IO, UserId]
  implicit val UserIdsDecoder = jsonOf[IO, List[UserId]]
  implicit val UserIdOptionDecoder = jsonOf[IO, Option[UserId]]
  implicit val UserDecoder = jsonOf[IO, User]
  implicit val UTCTimestampDecoder = jsonOf[IO, UTCTimestamp]
  implicit val PseudoDecoder = jsonOf[IO,Pseudo]
  implicit val ProfileDecoder = jsonOf[IO,Profile]
  implicit val ProfileOptionDecoder = jsonOf[IO,Option[Profile]]
  implicit val UserProfileDecoder = jsonOf[IO,(User, Profile)]
  implicit val UserProfileOptionDecoder = jsonOf[IO, Option[(User,Profile)]]
  // Ranking service
  implicit val RankDecoder = jsonOf[IO, Rank]
  implicit val ScoreBreakdownDecoder = jsonOf[IO, ScoreBreakdown]
  // Notification service
  implicit val NotificationIdDecoder = jsonOf[IO, NotificationId]
  implicit val NotificationDecoder = jsonOf[IO, Notification]
  implicit val NotificationOptionDecoder = jsonOf[IO, Option[Notification]]
  implicit val NotificationListDecoder = jsonOf[IO, List[Notification]]
//  implicit val NotificationContentDecoder = {
//    implicit val decoder: Decoder[NotificationContent] =
//      List[Decoder[NotificationContent]](
//        Decoder[PlatformMessageNotification].widen,
//        Decoder[EventNotification].widen,
//        Decoder[PostNotification].widen,
//        Decoder[PollNotification].widen,
//        Decoder[ChallengeNotification].widen
//      ).reduceLeft(_ or _)
//    jsonOf[IO, NotificationContent]
//  }
  implicit val NotificationContentDecoder = jsonOf[IO, NotificationContent]
  // FeedService
  implicit val FeedDecoder = jsonOf[IO, Feed]
  // WallService
  implicit val WallDecoder = jsonOf[IO, Wall]
  // Challenge
  implicit val ChallengeIdDecoder = jsonOf[IO, ChallengeId]
  implicit val ChallengeIdOptionDecoder = jsonOf[IO, Option[ChallengeId]]
  // Tip
  implicit val TipIdDecoder = jsonOf[IO, TipId]
  implicit val TipIdOptionDecoder = jsonOf[IO, Option[TipId]]
  // Poll
  implicit val PollIdDecoder = jsonOf[IO, PollId]
  implicit val PollIdOptionDecoder = jsonOf[IO, Option[PollId]]
  implicit val PollDecoder = jsonOf[IO, Poll]
  implicit val PollOptionDecoder = jsonOf[IO, PollOption]
  implicit val PollStatsDecoder = jsonOf[IO, PollStats]
  implicit val PollStatsEntryDecoder = jsonOf[IO, PollStatsEntry]
  implicit val PollAnswerOptionDecoder = jsonOf[IO, Option[PollAnswer]]
  // EventService
  implicit val EventIdDecoder = jsonOf[IO, EventId]
  implicit val EventIdListDecoder = jsonOf[IO, List[EventId]]
  implicit val EventIdOptionDecoder = jsonOf[IO, Option[EventId]]
  implicit val DurationDecoder = jsonOf[IO, Duration]
  implicit val ScheduleDecoder = jsonOf[IO, Schedule]
  implicit val CountryDecoder = jsonOf[IO, Country] // FIXME country will need a handmade decoder ...
  implicit val LatLongDecoder = jsonOf[IO, LatLong]
  implicit val LocationDecoder = jsonOf[IO, Location]
  implicit val EventDecoder = jsonOf[IO, Event]
  implicit val EventListDecoder = jsonOf[IO, List[Event]]
  implicit val EventOptionDecoder = jsonOf[IO, Option[Event]]
  // FollowerService
  implicit val UserIdSetDecoder = jsonOf[IO, Set[UserId]]
  // PostService
  implicit val UrlDecoder = jsonOf[IO, Url]
  implicit val LikeDecoder = jsonOf[IO, Like]
  implicit val HashtagDecoder = jsonOf[IO, Hashtag]
  implicit val SourceDecoder = jsonOf[IO, Source]
  implicit val SourceOptionDecoder = jsonOf[IO, Option[Source]]
  implicit val SourceListDecoder = jsonOf[IO, List[Source]]
  implicit val SourceListOptionDecoder = jsonOf[IO, Option[List[Source]]]
  implicit val HashtagSetDecoder = jsonOf[IO, Set[Hashtag]]
  implicit val PostIdDecoder = jsonOf[IO, PostId]
  implicit val PostIdSetDecoder = jsonOf[IO, Set[PostId]]
  implicit val PostIdOptionDecoder = jsonOf[IO, Option[PostId]]
  implicit val PostDecoder = jsonOf[IO, Post]
  implicit val PostOptionDecoder = jsonOf[IO, Option[Post]]
  implicit val PostListDecoder = jsonOf[IO, List[Post]]
  // TipService
  implicit val TipOptionDecoder = jsonOf[IO, Option[Tip]]
  implicit val TipIdSetDecoder = jsonOf[IO, Set[TipId]]
  // ChallengeService
  implicit val ChallengeOptionDecoder = jsonOf[IO, Option[Challenge]]
  implicit val ChallengeIdSetDecoder = jsonOf[IO, Set[ChallengeId]]
  implicit val ChallengeStatusDecoder = jsonOf[IO, ChallengeStatus]
  implicit val ChallengeOutcomeStatusDecoder = jsonOf[IO, ChallengeOutcomeStatus]
  // PinService
  implicit val PinnedPostDecoder = jsonOf[IO, PinnedPost]
  implicit val PinnedPosts = jsonOf[IO, List[PinnedPost]]
  // HashtagService
  implicit val IntHashtagListDecoder = jsonOf[IO, List[(Int,Hashtag)]]
  // ConversationService
  implicit val MessageIdDecoder = jsonOf[IO, MessageId]
  implicit val MessageIdListDecoder = jsonOf[IO, List[MessageId]]
  implicit val MessageDecoder = jsonOf[IO, Message]
  implicit val MessageListDecoder = jsonOf[IO, List[Message]]
  implicit val ConversationIdDecoder = jsonOf[IO, ConversationId]
  // PartnershipService
  implicit val PartnerIdDecoder = jsonOf[IO, PartnerId]
  implicit val PartnerDecoder = jsonOf[IO, Partner]
  implicit val PartnerOptionDecoder = jsonOf[IO, Option[Partner]]
  // AggregatedService
  implicit val AggregatedPostInfoListDecoder = jsonOf[IO, List[AggregatedPostInfo]]
  implicit val AggregatedPostInfoDecoder = jsonOf[IO, AggregatedPostInfo]
  implicit val EventInfoDecoder = jsonOf[IO, EventInfo]
  implicit val EventInfoOptionDecoder = jsonOf[IO, Option[EventInfo]]
  implicit val ChallengeInfoDecoder = jsonOf[IO, ChallengeInfo]
  implicit val ChallengeInfoOptionDecoder = jsonOf[IO, Option[ChallengeInfo]]
  implicit val PollInfoDecoder = jsonOf[IO, PollInfo]
  implicit val PollInfoOptionDecoder = jsonOf[IO, Option[PollInfo]]
  implicit val TipInfoDecoder = jsonOf[IO, TipInfo]
  implicit val TipInfoOptionDecoder = jsonOf[IO, Option[TipInfo]]
  implicit val RepostInfoDecoder = jsonOf[IO, RepostInfo]
  implicit val RepostInfoOptionDecoder = jsonOf[IO, Option[RepostInfo]]
  implicit val FreeTextInfoDecoder = jsonOf[IO, FreeTextInfo]
  implicit val FreeTextInfoOptionDecoder = jsonOf[IO, Option[FreeTextInfo]]

}
