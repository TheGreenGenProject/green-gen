package org.greengen.http.tip

import cats.effect.IO
import org.greengen.core.Source
import org.greengen.core.tip.{Tip, TipId, TipService}
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper._
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client

class HttpTipServiceClient(httpClient: Client[IO], root: Uri) extends TipService[IO] {

  override def create(author: UserId, content: String, sources: List[Source]): IO[TipId] =
    httpClient.expect[TipId](post(root / "tip" / "new",
      "user-id" -> author,
      "content" -> content,
      "sources" -> sources))

  override def byId(tipId: TipId): IO[Option[Tip]] =
    httpClient.expect[Option[Tip]](root / "tip" /"by-id" / tipId.value.uuid)

  override def byAuthor(author: UserId): IO[Set[TipId]] =
    httpClient.expect[Set[TipId]](root / "tip" /"by-author" / author.value.uuid)

}
