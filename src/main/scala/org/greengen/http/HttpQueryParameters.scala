package org.greengen.http

import org.greengen.core.Coordinate.{LatLong, Latitude, Longitude}
import org.greengen.core.challenge.{ChallengeId, SuccessMeasure}
import org.greengen.core.event.EventId
import org.greengen.core.poll.{PollId, PollOption}
import org.greengen.core.post.PostId
import org.greengen.core.tip.TipId
import org.greengen.core.user.{Pseudo, UserId}
import org.greengen.core._
import org.greengen.core.notification.NotificationId
import org.greengen.core.registration.ValidationCode
import org.http4s.{ParseFailure, QueryParamDecoder}
import org.http4s.dsl.io.QueryParamDecoderMatcher


object HttpQueryParameters {

  private[http] object UserIdQueryParamMatcher extends QueryParamDecoderMatcher[UserId]("user-id")
  private[http] object OwnerIdQueryParamMatcher extends QueryParamDecoderMatcher[UserId]("owner-id")
  private[http] object ParticpantIdQueryParamMatcher extends QueryParamDecoderMatcher[UserId]("participant-id")
  private[http] object FollowerQueryParamMatcher extends QueryParamDecoderMatcher[UserId]("follower-id")
  private[http] object FollowedQueryParamMatcher extends QueryParamDecoderMatcher[UserId]("followed-id")
  private[http] implicit lazy val userIdQueryParamDecoder: QueryParamDecoder[UserId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(UserId(_))

  private[http] object EventIdQueryParamMatcher extends QueryParamDecoderMatcher[EventId]("event-id")
  private[http] implicit lazy val eventIdQueryParamDecoder: QueryParamDecoder[EventId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(EventId(_))

  private[http] object PostIdQueryParamMatcher extends QueryParamDecoderMatcher[PostId]("post-id")
  private[http] implicit lazy val postIdQueryParamDecoder: QueryParamDecoder[PostId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(PostId(_))

  private[http] object PollIdQueryParamMatcher extends QueryParamDecoderMatcher[PollId]("poll-id")
  private[http] implicit lazy val pollIdQueryParamDecoder: QueryParamDecoder[PollId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(PollId(_))

  private[http] object ChallengeIdQueryParamMatcher extends QueryParamDecoderMatcher[ChallengeId]("challenge-id")
  private[http] implicit lazy val challengeIdQueryParamDecoder: QueryParamDecoder[ChallengeId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(ChallengeId(_))

  private[http] object TipIdQueryParamMatcher extends QueryParamDecoderMatcher[TipId]("tip-id")
  private[http] implicit lazy val tipIdQueryParamDecoder: QueryParamDecoder[TipId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(TipId(_))

  private[http] object HashtagsQueryParamMatcher extends QueryParamDecoderMatcher[Set[Hashtag]]("hashtags")
  private[http] implicit lazy val hashtagsQueryParamDecoder: QueryParamDecoder[Set[Hashtag]] = QueryParamDecoder[String]
    .map(_.split('+').map(Hashtag(_)).toSet)

  private[http] object TitleQueryParamMatcher extends QueryParamDecoderMatcher[String]("title")
  private[http] object ContentQueryParamMatcher extends QueryParamDecoderMatcher[String]("content")

  private[http] val MyselfRE = "myself".r
  private[http] val PostRE = "post\\((.+)\\)".r
  private[http] val WebRE = "url\\((.+)\\)".r
  private[http] val AcademicRE = "academic\\((.+)\\)".r
  private[http] object SourceListQueryParamMatcher extends QueryParamDecoderMatcher[List[Source]]("sources")
  private[http] implicit lazy val sourcesQueryParamDecoder: QueryParamDecoder[List[Source]] = QueryParamDecoder[String]
    .map(_.split('+').map {
      case MyselfRE() => MySelf
      case PostRE(id) => FromPost(PostId(UUID.unsafeFrom(id)))
      case WebRE(url) => Web(Url(url))
      case AcademicRE(ref) => AcademicReference(ref)
      case other => throw new IllegalArgumentException(s"Invalid source: $other")
    }.toList)

  private[http] object QuestionQueryParamMatcher extends QueryParamDecoderMatcher[String]("question")
  private[http] object PollOptionParamMatcher extends QueryParamDecoderMatcher[List[PollOption]]("options")
  private[http] implicit lazy val pollOptionListQueryParamDecoder: QueryParamDecoder[List[PollOption]] = QueryParamDecoder[String]
    .map(_.split('+').map(PollOption(_)).toList)

  private[http] object FlagReasonQueryParamMatcher extends QueryParamDecoderMatcher[Reason]("reason")
  private[http] implicit lazy val flagReasonQueryParamDecoder: QueryParamDecoder[Reason] = QueryParamDecoder[String]
    .map {
      case "Offensive" => Offensive
      case "Illegal" => Illegal
      case "PoliticallyBiased" => PoliticallyBiased
      case "Guideline" => Guideline
      // FIXME incomplete pattern matching - use Json request ?
      case other => throw new IllegalArgumentException(s"Invalid reason $other")
    }

  private[http] object MaxParticipantQueryParamMatcher extends QueryParamDecoderMatcher[Int]("max-participant")
  private[http] object DescriptionQueryParamMatcher extends QueryParamDecoderMatcher[String]("description")

  private[http] val OnLineLocationRE = "url\\((.+)\\)".r
  private[http] val GeoLocationRE = "geoloc\\((.+),(.+)\\)".r
  private[http] val GoogleMapLocationRE = "map\\(https?://www\\.google\\.com/maps/.+\\)".r
  private[http] val OpenStreetMapLocationRE = "map\\(https?://www\\.openstreetmap\\.org/.+\\)".r
  private[http] val AddressRE = "address\\((.*),(.*),(.+)\\)".r
  private[http] object LocationQueryParamMatcher extends QueryParamDecoderMatcher[Location]("location")
  private[http] implicit lazy val locationOnlineQueryParamDecoder: QueryParamDecoder[Location] = QueryParamDecoder[String]
    .map {
      case OnLineLocationRE(url) =>
        Online(Url(url))
      case GoogleMapLocationRE(loc) =>
        MapUrl(Url(s"https://www.google.com/maps/$loc"))
      case OpenStreetMapLocationRE(loc) =>
        MapUrl(Url(s"https://www.openstreetmap.org/maps/$loc"))
      case GeoLocationRE(lat, long) =>
        GeoLocation(LatLong(Latitude(lat.toDouble), Longitude(long.toDouble)))
      case AddressRE(add, zip, country) => Address(
        Option.when(add.nonEmpty)(add),
        Option.when(zip.nonEmpty)(zip),
        Country(country))
      case other =>
        throw new IllegalArgumentException(s"Invalid location: $other")
    }

  private[http] val OneOffRE = "oneoff\\((\\d+),(\\d+)\\)".r
  private[http] val RecurringRE = "rec\\((\\d+),(\\d+),(\\d+),(\\d+)\\)".r
  private[http] object ScheduleQueryParamMatcher extends QueryParamDecoderMatcher[Schedule]("schedule")
  private[http] implicit lazy val scheduleQueryParamDecoder: QueryParamDecoder[Schedule] = QueryParamDecoder[String]
    .map {
      case OneOffRE(start, end) =>
        OneOff(UTCTimestamp(start.toLong), UTCTimestamp(end.toLong))
      case RecurringRE(start, duration, every, until) =>
        Recurring(UTCTimestamp(start.toLong), Duration(duration.toLong), Duration(every.toLong), UTCTimestamp(until.toLong))
    }

  // SuccessMeasureQueryParamMatcher
  private[http] val SuccessMeasureRE = "success\\((\\d+),(\\d+),(\\d+)\\)".r
  private[http] object SuccessMeasureQueryParamMatcher extends QueryParamDecoderMatcher[SuccessMeasure]("success")
  private[http] implicit lazy val successMeasureQueryParamDecoder: QueryParamDecoder[SuccessMeasure] = QueryParamDecoder[String]
    .map {
      case SuccessMeasureRE(maxFailure, maxSkip, maxPartial) =>
        SuccessMeasure(maxFailure.toInt, maxSkip.toInt, maxPartial.toInt)
    }

  private[http] object NotificationIdQueryParamMatcher extends QueryParamDecoderMatcher[NotificationId]("notif-id")
  private[http] implicit lazy val notificationIdQueryParamDecoder: QueryParamDecoder[NotificationId] = QueryParamDecoder[String]
    .map(UUID.unsafeFrom)
    .map(NotificationId(_))

  private[http] object MessageQueryParamMatcher extends QueryParamDecoderMatcher[String]("message")
  private[http] object ReasonQueryParamMatcher extends QueryParamDecoderMatcher[String]("reason")
  private[http] object ProfileIntroductionQueryParamMatcher extends QueryParamDecoderMatcher[String]("profile-introduction")
  private[http] object PseudoQueryParamMatcher extends QueryParamDecoderMatcher[Pseudo]("pseudo")
  private[http] implicit lazy val pseudoParamDecoder: QueryParamDecoder[Pseudo] = QueryParamDecoder[String]
    .emap(Pseudo.from(_).left.map(err => ParseFailure(err, err)))
  private[http] object ValidationCodeQueryParamMatcher extends QueryParamDecoderMatcher[ValidationCode]("validation-code")
  private[http] implicit lazy val validationCodeParamDecoder: QueryParamDecoder[ValidationCode] = QueryParamDecoder[String]
    .emap(ValidationCode.from(_).left.map(err => ParseFailure(err, err)))

  // TODO add check on hexa string length - since it is a MD5
  private[http] object EmailHashQueryParamMatcher extends QueryParamDecoderMatcher[Hash]("email-hash")
  private[http] object PasswordHashQueryParamMatcher extends QueryParamDecoderMatcher[Hash]("password-hash")
  implicit lazy val hashQueryParamDecoder: QueryParamDecoder[Hash] = QueryParamDecoder[String]
    .map(Base16.decodeFrom(_).getOrElse(throw new RuntimeException("Invalid hexadecimal string")))
    .map(Hash.safeFrom)

  private[http] object TokenQueryParamMatcher extends QueryParamDecoderMatcher[Token]("token")
  implicit lazy val tokenQueryParamDecoder: QueryParamDecoder[Token] = QueryParamDecoder[String].map(Token.unsafeFrom)

}
