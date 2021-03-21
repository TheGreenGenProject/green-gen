package org.greengen.http.follower

import cats.effect.IO
import org.greengen.core.follower.FollowerService
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client

class HttpFollowerServiceClient(httpClient: Client[IO], root: Uri) extends FollowerService[IO] {

  override def startFollowing(src: UserId, dst: UserId): IO[Unit] =
    httpClient.expect[Unit](post(root / "followers" / "add",
      "follower" -> src.value.uuid,
      "following" -> dst.value.uuid))

  override def stopFollowing(src: UserId, dst: UserId): IO[Unit] =
    httpClient.expect[Unit](post(root / "followers" / "remove",
      "follower" -> src.value.uuid,
      "following" -> dst.value.uuid))

  override def followers(id: UserId): IO[Set[UserId]] =
    httpClient.expect[Set[UserId]](root / "followers" / "all" / id.value.uuid)

  override def countFollowers(id: UserId): IO[Int] =
    httpClient.expect[Int](root / "followers" / "count" / id.value.uuid)

  override def following(id: UserId): IO[Set[UserId]] =
    httpClient.expect[Set[UserId]](root / "following" / "all" / id.value.uuid)

  override def countFollowing(id: UserId): IO[Int] =
    httpClient.expect[Int](root / "following" / "count" / id.value.uuid)

}

