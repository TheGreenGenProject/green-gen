package org.greengen.http.hashtag

import cats.effect.IO
import org.greengen.core.hashtag.HashtagService
import cats.effect.IO
import org.greengen.core.Hashtag
import org.greengen.core.follower.FollowerService
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client

class HttpHashtagServiceClient(httpClient: Client[IO], root: Uri) extends HashtagService[IO] {

  override def startFollowing(src: UserId, hashtag: Hashtag): IO[Unit] =
    httpClient.expect[Unit](post(root / "hashtag" / "followers" / "add" / hashtag.value)) // FIXME

  override def stopFollowing(src: UserId, hashtag: Hashtag): IO[Unit] =
    httpClient.expect[Unit](post(root / "hashtag" / "followers" / "remove" / hashtag.value)) // FIXME

  override def followers(hashtag: Hashtag): IO[Set[UserId]] =
    throw new NotImplementedError()

  override def countFollowers(hashtag: Hashtag): IO[Int] =
    httpClient.expect[Int](root / "hashtag" / "followers" / "count" / hashtag.value)

  override def hashtagsfollowedBy(userId: UserId): IO[Set[Hashtag]] =
    httpClient.expect[Set[Hashtag]](root / "hashtag" / "followed-by" / userId.value.uuid)

  override def trendByFollowers(n: Int): IO[List[(Int, Hashtag)]] =
    httpClient.expect[List[(Int, Hashtag)]](root / "hashtag" / "trend" / "by-followers" / n.toString)

}
