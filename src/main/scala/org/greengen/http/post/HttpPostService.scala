package org.greengen.http.post

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.post._
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, _}
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scala.util.Try

object HttpPostService {

  val PageSize = 10

  def routes(clock: Clock, service: PostService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "post" / "by-id" / UUIDVar(id) as _ =>
      service.byId(PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "post" / "by-content" / "challenge" / UUIDVar(id) as _ =>
      service.byContent(ChallengeId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "post" / postType / "by-author" / UUIDVar(id) / IntVar(page) as _ =>
      val searchPostType = SearchPostType.fromString(postType)
          .getOrElse(AllPosts)
      service.byAuthor(UserId(UUID.from(id)), searchPostType, Page(page, by=PageSize))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "post" / postType / "by-hashtag" / tags / IntVar(page) as _ =>
      val searchPostType = SearchPostType.fromString(postType)
        .getOrElse(AllPosts)
      Try(tags.split('+').map(Hashtag(_)).toSet)
        .fold( _ => BadRequest(),
              service.byHashtags(_, searchPostType, Page(page, by=PageSize))
                .flatMap(r => Ok(r.asJson)))
    case GET -> Root / "post" / "is-flagged" / UUIDVar(id) as _ =>
      service.isFlagged(PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "hashtag" / "trend" / "by-posts" / IntVar(n) as _ =>
      service.trendByHashtags(n).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "post" / "flag" :?
      PostIdQueryParamMatcher(postId) +&
      FlagReasonQueryParamMatcher(reason) as userId =>
      service.flag(userId, postId, reason).flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "repost" :?
      PostIdQueryParamMatcher(postId) as userId =>
      service.repost(userId, postId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "new" / "event" :?
        EventIdQueryParamMatcher(eventId) +&
        HashtagsQueryParamMatcher(hashtags) as userId =>
      service.post(EventPost(PostId.newId, userId, eventId, clock.now(), hashtags))
        .flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "new" / "repost" :?
      PostIdQueryParamMatcher(postId) +&
      HashtagsQueryParamMatcher(hashtags) as userId =>
      service.post(RePost(PostId.newId, userId, postId, clock.now(), hashtags))
        .flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "new" / "poll" :?
        PollIdQueryParamMatcher(pollId) +&
        HashtagsQueryParamMatcher(hashtags) as userId =>
      service.post(PollPost(PostId.newId, userId, pollId, clock.now(), hashtags))
        .flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "new" / "challenge" :?
      ChallengeIdQueryParamMatcher(challengeId) +&
      HashtagsQueryParamMatcher(hashtags) as userId =>
      service.post(ChallengePost(PostId.newId, userId, challengeId, clock.now(), hashtags))
        .flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "new" / "tip" :?
      TipIdQueryParamMatcher(tipId) +&
      HashtagsQueryParamMatcher(hashtags) as userId =>
      service.post(TipPost(PostId.newId, userId, tipId, clock.now(), hashtags))
        .flatMap(r => Ok(r.asJson))
    case POST -> Root / "post" / "new" / "free-text" :?
      HashtagsQueryParamMatcher(hashtags) +&
      ContentQueryParamMatcher(content) +&
      SourceListQueryParamMatcher(sources) as userId =>
      service.post(FreeTextPost(PostId.newId, userId, content, sources, clock.now(), hashtags))
        .flatMap(r => Ok(r.asJson))
  }

}
