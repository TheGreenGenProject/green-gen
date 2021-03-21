package org.greengen.http.pin

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.pin.{PinService, PinnedPost}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client


class HttpPinServiceClient(httpClient: Client[IO], root: Uri) extends PinService[IO] {

  override def pin(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "pin" / "add",
      "user-id" -> userId.value.uuid,
      "post-id" -> postId.value.uuid))

  override def unpin(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "pin" / "remove",
      "user-id" -> userId.value.uuid,
      "post-id" -> postId.value.uuid))

  override def isPinned(userId: UserId, postId: PostId): IO[Boolean] =
    httpClient.expect[Boolean](root / "pin" / "is-pinned" /  postId.value.uuid)

  override def byUser(userId: UserId, page: Page): IO[List[PinnedPost]] =
    httpClient.expect[List[PinnedPost]](root / "pin" / "pinned" / page.n.toString)

}