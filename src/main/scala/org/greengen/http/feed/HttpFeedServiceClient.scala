package org.greengen.http.feed

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.feed.{Feed, FeedService}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client


class HttpFeedServiceClient(httpClient: Client[IO], root: Uri) extends FeedService[IO] {

  override def feed(userId: UserId, page: Page): IO[Feed] =
    httpClient.expect[Feed](root / "feed" / userId.value.uuid / page.n.toString)

  override def addToFeed(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "feed" / "add",
      "user-id" -> userId.value.uuid,
    "post-id" -> postId.value.uuid))

  override def addToFollowersFeed(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "feed" / "add-to-followers",
      "user-id" -> userId.value.uuid,
      "post-id" -> postId.value.uuid))

  override def hasPostsAfter(userId: UserId, lastPostId: PostId): IO[Boolean] =
    httpClient.expect[Boolean](root / "feed" / "has-new-posts" / lastPostId.value.uuid) // FIXME

  override def hasPosts(userId: UserId): IO[Boolean] =
    httpClient.expect[Boolean](root / "feed" / "has-posts") // FIXME
}