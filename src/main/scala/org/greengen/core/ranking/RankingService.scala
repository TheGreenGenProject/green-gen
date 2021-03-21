package org.greengen.core.ranking

import cats.effect.IO
import org.greengen.core.user.UserId

// Returns the current rank of a user
trait RankingService[F[_]] {

  def rank(userId: UserId): IO[Rank]

  def score(userId: UserId): IO[ScoreBreakdown]

}
