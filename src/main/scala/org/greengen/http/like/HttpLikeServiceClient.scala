package org.greengen.http.like

import cats.effect.IO
import org.greengen.core.like.{Like, LikeService}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client


class HttpLikeServiceClient(httpClient: Client[IO], root: Uri) extends LikeService[IO] {

  override def like(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "like" / "add",
      "user-id" -> userId.value.uuid,
      "post-id" -> postId.value.uuid))

  override def unlike(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "like" / "remove",
      "user-id" -> userId.value.uuid,
      "post-id" -> postId.value.uuid))

  override def isLiked(userId: UserId, postId: PostId): IO[Boolean] =
    httpClient.expect[Boolean](root / "like" / "is-liked" / postId.value.uuid)

  override def countLikes(postId: PostId): IO[Like] =
    httpClient.expect[Like](root / "like" / postId.value.uuid)

}
