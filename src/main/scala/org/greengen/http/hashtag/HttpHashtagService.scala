package org.greengen.http.hashtag

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.hashtag.HashtagService
import org.greengen.core.user.UserId
import org.greengen.core.{Hashtag, UUID}
import org.http4s.AuthedRoutes
import org.http4s.circe._
import org.http4s.dsl.io._


object HttpHashtagService {

  def routes(service: HashtagService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "hashtag" / "followers" / "count" / ht as _ =>
      service.countFollowers(Hashtag(ht)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "hashtag" / "followed" as userId =>
      service.hashtagsfollowedBy(userId).flatMap(r => Ok(r.asJson))
    case GET -> Root / "hashtag" / "trend" / "by-followers" / IntVar(n) as _ =>
      service.trendByFollowers(n).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "hashtag" / "followers" / "add" / ht as followerId =>
      service.startFollowing(followerId, Hashtag(ht)).flatMap(r => Ok(r.asJson))
    case POST -> Root / "hashtag" / "followers" / "remove" / ht as followerId =>
      service.stopFollowing(followerId, Hashtag(ht)).flatMap(r => Ok(r.asJson))
  }

}
