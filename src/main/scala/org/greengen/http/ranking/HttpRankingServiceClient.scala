package org.greengen.http.ranking

import cats.effect.IO
import org.greengen.core.ranking.{Rank, RankingService, ScoreBreakdown}
import org.greengen.core.user.UserId
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client


class HttpRankingServiceClient(httpClient: Client[IO], root: Uri) extends RankingService[IO] {

  override def rank(userId: UserId): IO[Rank] =
    httpClient.expect[Rank](root / "rank" / userId.value.uuid)

  override def score(userId: UserId): IO[ScoreBreakdown] =
    httpClient.expect[ScoreBreakdown](root / "rank" / "breakdown" / userId.value.uuid)

}
