package org.greengen.http.wall

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.core.wall.{Wall, WallService}
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client


class HttpWallServiceClient(httpClient: Client[IO], root: Uri) extends WallService[IO] {

  override def wall(userId: UserId, page: Page): IO[Wall] =
    httpClient.expect[Wall](root / "wall" / userId.value.uuid / page.n.toString)

  override def addToWall(userId: UserId, postId: PostId): IO[Unit] =
    httpClient.expect[Unit](post(root / "wall" / "add",
      "user-id" -> userId.value.uuid,
      "post-id" -> postId.value.uuid))

}