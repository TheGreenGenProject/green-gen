package org.greengen.db.mongo

import org.greengen.core.challenge.{Challenge, ChallengeContent, ChallengeId, ChallengeStepReportEntry, ChallengeStepReportStatus, Failure, Partial, Skipped, Success, SuccessMeasure}
import org.greengen.core.event.EventId
import org.greengen.core.poll.PollId
import org.greengen.core.post._
import org.greengen.core.tip.{Tip, TipId}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}
import org.greengen.core.{Duration, Hash, Hashtag, OneOff, Recurring, Schedule, UTCTimestamp, UUID}
import org.greengen.db.mongo.Conversions.hexToBytes
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonInt64, BsonString}
import org.mongodb.scala.bson.collection.Document

import scala.jdk.CollectionConverters._


object Schema {

  def userIdToDocument(userId: UserId): Document =
    Document("user_id" -> userId.value.uuid)

  def docToUserId(doc: Document): Either[String, UserId] = for {
    str <- Option(doc.getString("user_id")).toRight(s"No field user_id found in $doc")
    uuid <- UUID.from(str).toRight(s"Invalid UUID $str")
  } yield UserId(uuid)

  def userProfileToDoc(user: User, profile: Profile): Document =
    Document(
      "user_id"       -> user.id.value.uuid,
      "credentials"   -> Document(
        "email_hash"    -> user.emailHash.toString,
        "password_hash" -> user.passwordHash.toString),
      "enabled"       -> user.enabled,
      "profile"       -> profileToDoc(profile))

  def docToUserProfile(doc: Document): Either[String, (User, Profile)] = for {
    uuid <- Option(doc.getString("user_id"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field user_id or invalid UUID found in $doc")
    credentials <- doc.get("credentials").map(_.asDocument)
      .toRight(s"No field credentials found in $doc")
    emailHash <- Option(credentials.get("email_hash").asString.getValue)
      .map(hexToBytes)
      .map(Hash(_))
      .toRight(s"No field credentials.email_hash found in $doc")
    passwordHash <- Option(credentials.get("password_hash").asString.getValue)
      .map(hexToBytes)
      .map(Hash(_))
      .toRight(s"No field credentials.password_hash found in $doc")
    enabled <- Option(doc.getBoolean("enabled"))
      .toRight(s"No field enabled found in $doc")
    profile <- docToProfile(doc)
  } yield (User(uuid, emailHash, passwordHash, enabled), profile)

  def profileToDoc(profile: Profile): Document =
    Document(
      "pseudo"   -> profile.pseudo.value,
      "intro"    -> profile.intro,
      "since"    -> profile.since.value,
      "verified" -> profile.verified
    )

  def docToProfile(doc: Document): Either[String, Profile] = for {
    uuid <- Option(doc.getString("user_id"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field 'user_id' or invalid UUID found in $doc")
    profile <- doc.get("profile").map(_.asDocument)
        .toRight(s"No field 'profile' found in $doc")
    pseudo <- Option(profile.get("pseudo").asString.getValue)
      .map(Pseudo(_))
      .toRight(s"No field 'pseudo' found in $doc")
    intro <- Right(Option(profile.get("intro").asString.getValue))
    since <- Option(profile.get("since").asInt64.getValue)
      .map(UTCTimestamp(_))
      .toRight(s"No field 'since' found in $doc")
    verified <- Option(profile.get("verified").asBoolean.getValue)
      .toRight(s"No field 'verified' found in $doc")
  } yield Profile(uuid, pseudo, since, intro, verified)


  def postToDoc(post: Post): Document = {
    val builder = Document.builder
      .addOne("post_id"  -> BsonString(post.id.value.uuid))
      .addOne("author"   -> BsonString(post.author.value.uuid))
      .addOne("created"  -> BsonInt64(post.created.value))
      .addOne("hashtags" -> BsonArray.fromIterable(post.hashtags.map(_.value).map(BsonString(_)))
    )
    post match {
      case post: RePost        => builder.addOne("original_post_id" -> BsonString(post.originalId.value.uuid))
      case post: FreeTextPost  => builder.addOne("free_text" -> BsonDocument("content" -> post.content))
      case post: ChallengePost => builder.addOne("challenge_id" -> BsonString(post.challenge.value.uuid))
      case post: TipPost       => builder.addOne("tip_id"  -> BsonString(post.tip.value.uuid))
      case post: PollPost      => builder.addOne("poll_id" -> BsonString(post.poll.id.uuid))
    }
    builder.result()
  }

  def docToPost(doc: Document): Either[String, Post] = for {
    uuid <- Option(doc.getString("post_id"))
      .flatMap(UUID.from)
      .map(PostId(_))
      .toRight(s"No field 'post_id' or invalid UUID found in $doc")
    author <- Option(doc.getString("author"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field 'author' or invalid UUID found in $doc")
    created <- Option(doc.getLong("created"))
      .map(UTCTimestamp(_))
      .toRight(s"No field 'since' found in $doc")
    hashtags <- Option(doc.getList("hashtags", classOf[String]).asScala)
      .map(_.map(Hashtag(_)))
      .toRight(s"No field 'hashtags' found in $doc")
    post <- postFrom(doc, uuid, author, created, hashtags.toSet)
  } yield post

  // db.posts.challenges.insert({
  //    "challenge_id": "2afbe95a-754f-4a47-888-87cb691141bd",
  //    "author": "2afbe95a-754f-4a47-888-87cb691141bd",
  //    "title": "This is a title",
  //    "content": "This is the content of a free text post",
  //    "created": 1621364129863,
  //    "schedule": {
  //        "start": 1621364129863,
  //        "duration": 4129863,
  //        "every": 4129863,
  //        "end": 1621364129863
  //    },
  //    "measure": {
  //        "max_failure": 1,
  //        "max_skip": 2,
  //        "max_partial": 2,
  //    }
  //});
  def docToChallenge(doc: Document): Either[String, Challenge] = for {
    uuid <- Option(doc.getString("challenge_id"))
      .flatMap(UUID.from)
      .map(ChallengeId(_))
      .toRight(s"No field 'challenge_id' or invalid UUID found in $doc")
    author <- Option(doc.getString("author"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field 'author' or invalid UUID found in $doc")
    title <- Option(doc.getString("title"))
      .toRight(s"No field 'title' found in $doc")
    created <- Option(doc.getLong("created"))
      .map(UTCTimestamp(_))
      .toRight(s"No field 'created' found in $doc")
    content <- Option(doc.getString("content"))
      .toRight(s"No field 'content' found in $doc")
    schedule <- doc.get("schedule")
      .map(_.asDocument)
      .toRight(s"No field 'schedule' found in $doc")
      .flatMap(docToSchedule(_))
    measure <- doc.get("measure")
      .map(_.asDocument)
      .toRight(s"No field 'measure' found in $doc")
      .flatMap(docToSuccessMeasure(_))
  } yield Challenge(uuid, author, created, schedule, ChallengeContent(title, content), measure)

  def challengeToDoc(challenge: Challenge): Document =
    Document(
      "challenge_id" -> challenge.id.value.uuid,
      "author" -> challenge.author.value.uuid,
      "title" -> challenge.content.title,
      "content" -> challenge.content.description,
      "created" -> challenge.created.value,
      "schedule" -> scheduleToDoc(challenge.schedule),
      "measure" -> successMeasureToDoc(challenge.measure)
    )

  //    "schedule": {
  //        "start": 1621364129863,
  //        "duration": 4129863,
  //        "every": 4129863,
  //        "end": 1621364129863
  //    },
  def docToSchedule(doc: Document): Either[String, Schedule] = for {
    start <- Option(doc.getLong("start"))
      .map(UTCTimestamp(_))
      .toRight(s"No field 'start' found in $doc")
    duration <- Option(doc.getLong("duration"))
      .map(Duration(_))
      .toRight(s"No field 'duration' found in $doc")
    every <- Option(doc.getLong("every"))
      .map(Duration(_))
      .toRight(s"No field 'every' found in $doc")
    end <- Option(doc.getLong("end"))
      .map(UTCTimestamp(_))
      .toRight(s"No field 'end' found in $doc")
  } yield Recurring(start, duration, every, end)

  def scheduleToDoc(schedule: Schedule): Document =
    schedule match {
      case Recurring(start, duration, every, end) => Document(
        "start" -> start.value,
        "duration" -> duration.millis,
        "every" -> every.millis,
        "end" -> end.value
      )
      case OneOff(start, end) => Document(
        "start" -> start.value,
        "end" -> end.value
      )
    }

  //    "measure": {
  //        "max_failure": 1,
  //        "max_skip": 2,
  //        "max_partial": 2,
  //    }
  def docToSuccessMeasure(doc: Document): Either[String, SuccessMeasure] = for {
    maxFailure <- Option(doc.getInteger("max_failure"))
      .toRight(s"No field 'max_failure' found in $doc")
    maxSkip <- Option(doc.getInteger("max_skip"))
      .toRight(s"No field 'max_skip' found in $doc")
    maxPartial <- Option(doc.getInteger("max_partial"))
      .toRight(s"No field 'max_partial' found in $doc")
  } yield SuccessMeasure(
    maxFailure = maxFailure,
    maxSkip = maxSkip,
    maxPartial = maxPartial)

  def successMeasureToDoc(successMeasure: SuccessMeasure): Document =
    Document(
      "max_failure" -> successMeasure.maxFailure,
      "max_skip"    -> successMeasure.maxSkip,
      "max_partial" -> successMeasure.maxPartial
    )

  def challengeStepEntryToDoc(entry: ChallengeStepReportEntry): Document =
    Document(
      "step"      -> entry.step,
      "status"    -> ChallengeStepReportStatus.format(entry.status),
    )

  def docToChallengeStepEntry(doc: Document): Either[String, ChallengeStepReportEntry] = for {
    step <- Option(doc.getInteger("step"))
      .toRight(s"No field 'step' found in $doc")
    status <- Option(doc.getString("status"))
      .flatMap(ChallengeStepReportStatus.from(_))
      .toRight(s"No field 'status' or invalid value found in $doc")
  } yield ChallengeStepReportEntry(step, status)



  sealed trait AcceptanceStatus
  case object ChallengeNotYetAccepted extends AcceptanceStatus
  case object ChallengeAccepted extends AcceptanceStatus
  case object ChallengeRejected extends AcceptanceStatus
  case object ChallengeCancelled extends AcceptanceStatus
  case object InvalidStatus extends AcceptanceStatus
  object AcceptanceStatus {
    def format(status: AcceptanceStatus) = status match {
      case ChallengeAccepted       => "Accepted"
      case ChallengeNotYetAccepted => "NotYetAccepted"
      case ChallengeRejected       => "Rejected"
      case ChallengeCancelled      => "Cancelled"
      case InvalidStatus           => "Invalid"
    }
    def from(status: String): AcceptanceStatus = status match {
      case "Accepted"       =>  ChallengeAccepted
      case "NotYetAccepted" =>  ChallengeNotYetAccepted
      case "Rejected"       =>  ChallengeRejected
      case "Cancelled"      => ChallengeCancelled
      case _                => InvalidStatus
    }
  }

  case class Challengee(
    challengeId: ChallengeId,
    challengeeId: UserId,
    timestamp: UTCTimestamp,
    status: AcceptanceStatus)

  def docToChallengee(doc: Document): Either[String, Challengee] = for {
    uuid <- Option(doc.getString("challenge_id"))
      .flatMap(UUID.from)
      .map(ChallengeId(_))
      .toRight(s"No field 'challenge_id' or invalid UUID found in $doc")
    challengeeId <- Option(doc.getString("challengee_id"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field 'challengee_id' or invalid UUID found in $doc")
    timestamp <- Option(doc.getLong("timestamp"))
      .map(UTCTimestamp(_))
      .toRight(s"No field 'timestamp' found in $doc")
    status <- Option(doc.getString("status"))
      .map(AcceptanceStatus.from(_))
      .toRight(s"No field 'status' found in $doc")
  } yield Challengee(uuid, challengeeId, timestamp, status)

  def challengeeToDoc(challengee: Challengee): Document =
    Document(
      "challenge_id"  -> challengee.challengeId.value.uuid,
      "challengee_id" -> challengee.challengeeId.value.uuid,
      "timestamp"     -> challengee.timestamp.value,
      "status"        -> AcceptanceStatus.format(challengee.status)
    )


  def docToTip(doc: Document): Either[String, Tip] = for {
    uuid <- Option(doc.getString("tip_id"))
      .flatMap(UUID.from)
      .map(TipId(_))
      .toRight(s"No field 'tip_id' or invalid UUID found in $doc")
    author <- Option(doc.getString("author"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field 'author' or invalid UUID found in $doc")
    created <- Option(doc.getLong("created"))
      .map(UTCTimestamp(_))
      .toRight(s"No field 'created' found in $doc")
    content <- Option(doc.getString("content"))
      .toRight(s"No field 'content' found in $doc")
  } yield Tip(uuid, author, content, created, List())

  def tipToDoc(tip: Tip): Document =
    Document(
      "tip_id"  -> tip.id.value.uuid,
      "author"  -> tip.author.value.uuid,
      "content" -> tip.content,
      "created" -> tip.created.value
    )


  // Helpers

  // Dirty job of identifying the type of post and building it ...
  private[mongo] def postFrom(doc: Document,
                              postId: PostId,
                              author: UserId,
                              created: UTCTimestamp,
                              hashtags: Set[Hashtag]): Either[String, Post] = {
    if(doc.contains("free_text"))
      readFreeTextPost(doc, postId, author, created, hashtags)
    else if (doc.contains("tip_id"))
      readTipPost(doc, postId, author, created, hashtags)
    else if (doc.contains("challenge_id"))
      readChallengePost(doc, postId, author, created, hashtags)
    else if (doc.contains("poll_id"))
      readPollPost(doc, postId, author, created, hashtags)
    else if (doc.contains("original_post_id"))
      readRepost(doc, postId, author, created, hashtags)
    else if (doc.contains("event_id"))
      readEventPost(doc, postId, author, created, hashtags)
    else
      Left(s"Couldn't convert document into post $doc")
  }

  private[mongo] def readEventPost(doc: Document,
                                   postId: PostId,
                                   author: UserId,
                                   created: UTCTimestamp,
                                   hashtags: Set[Hashtag]): Either[String, EventPost] =
    for {
      eventId <- UUID.from(doc.getString("event_id"))
        .map(EventId(_))
        .toRight(s"No field 'event_id' in $doc")
    } yield EventPost(postId, author, eventId, created, hashtags)

  private[mongo] def readRepost(doc: Document,
                                postId: PostId,
                                author: UserId,
                                created: UTCTimestamp,
                                hashtags: Set[Hashtag]): Either[String, RePost] =
    for {
      originalPostId <- UUID.from(doc.getString("original_post_id"))
        .map(PostId(_))
        .toRight(s"No field 'original_post_id' in $doc")
    } yield RePost(postId, author, originalPostId, created, hashtags)

  private[mongo] def readPollPost(doc: Document,
                                  postId: PostId,
                                  author: UserId,
                                  created: UTCTimestamp,
                                  hashtags: Set[Hashtag]): Either[String, PollPost] =
    for {
      pollId <- UUID.from(doc.getString("poll_id"))
        .map(PollId(_))
        .toRight(s"No field 'challenge_id' in $doc")
    } yield PollPost(postId, author, pollId, created, hashtags)

  private[mongo] def readChallengePost(doc: Document,
                                       postId: PostId,
                                       author: UserId,
                                       created: UTCTimestamp,
                                       hashtags: Set[Hashtag]) =
    for {
      challengeId <- UUID.from(doc.getString("challenge_id"))
        .map(ChallengeId(_))
        .toRight(s"No field 'challenge_id' in $doc")
    } yield ChallengePost(postId, author, challengeId, created, hashtags)

  private[mongo] def readTipPost(doc: Document,
                                 postId: PostId,
                                 author: UserId,
                                 created: UTCTimestamp,
                                 hashtags: Set[Hashtag]): Either[String, TipPost] =
    for {
      tipId <- UUID.from(doc.getString("tip_id"))
        .map(TipId(_))
        .toRight(s"No field 'tip_id' in $doc")
    } yield TipPost(postId, author, tipId, created, hashtags)

  private[mongo] def readFreeTextPost(doc: Document,
                                      postId: PostId,
                                      author: UserId,
                                      created: UTCTimestamp,
                                      hashtags: Set[Hashtag]): Either[String, FreeTextPost] =
    for {
      freeText <- doc.get("free_text").map(_.asDocument)
        .toRight(s"No field 'free_text' in $doc")
      content <- Option(freeText.get("content").asString.getValue)
        .toRight(s"No field 'content' for free text post in $doc")
//      sources <- Option(freeText.get("sources").asArray().asScala.toList)
//        .toRight(s"No field 'sources' for free text post in $doc")
    } yield FreeTextPost(postId, author, content, List(), created, hashtags) // FIXME decode sourcess

}
