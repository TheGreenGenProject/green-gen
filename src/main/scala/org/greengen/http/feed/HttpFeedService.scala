package org.greengen.http.feed

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.feed.FeedService
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.core.{Page, UUID}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.greengen.http.HttpQueryParameters._

object HttpFeedService {

  val PageSize = 10

  def routes(service: FeedService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "feed" / UUIDVar(id) / IntVar(page) as _ =>
      service.feed(UserId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "feed" / "has-new-posts" / UUIDVar(id) as userId =>
      service.hasPostsAfter(userId, PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "feed" / "has-posts" as userId =>
      service.hasPosts(userId).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "feed" / "add" :?
      UserIdQueryParamMatcher(userId) +&
      PostIdQueryParamMatcher(postId) as _ =>
      service.addToFeed(userId, postId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "feed" / "add-to-followers" :?
      UserIdQueryParamMatcher(userId) +&
      PostIdQueryParamMatcher(postId) as _ =>
      service.addToFollowersFeed(userId, postId).flatMap(r => Ok(r.asJson))
  }

}
