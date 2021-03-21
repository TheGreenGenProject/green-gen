package org.greengen.http.follower

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.follower.FollowerService
import org.greengen.core.user.UserId
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.greengen.http.HttpQueryParameters._


object HttpFollowerService {

  def routes(service: FollowerService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "followers" / "all" / UUIDVar(id) as _ =>
      service.followers(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "followers" / "count" / UUIDVar(id) as _ =>
      service.countFollowers(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "following" / "all" / UUIDVar(id) as _ =>
      service.following(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "following" / "count" / UUIDVar(id) as _ =>
      service.countFollowing(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "followers" / "add" :?
      FollowedQueryParamMatcher(followedId) as followerId =>
      service.startFollowing(followerId, followedId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "followers" / "remove" :?
      FollowedQueryParamMatcher(followedId) as followerId =>
      service.stopFollowing(followerId, followedId).flatMap(r => Ok(r.asJson))
  }

}
