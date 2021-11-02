package org.greengen.impl.ranking

import cats.effect.IO
import org.greengen.core.{Clock, Duration, UTCTimestamp}
import org.greengen.core.ranking.{Rank, RankingService, ScoreBreakdown}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._


object CachedRankingService {
  def withCache(clock: Clock, retention: FiniteDuration = 15.minutes)
               (rankingService: RankingService[IO]): RankingService[IO] =
    new CachedRankingService(retention)(clock, rankingService)
}

// Cache ranking calculation as they can be time consuming when backed by a database
private[ranking] class CachedRankingService(retention: FiniteDuration)
                                           (clock: Clock, rankingService: RankingService[IO]) extends RankingService[IO] {

  val ranks = new TrieMap[UserId, (UTCTimestamp, Rank)]
  val scores = new TrieMap[UserId, (UTCTimestamp, ScoreBreakdown)]

  override def rank(userId: UserId): IO[Rank] =
    ranks.get(userId) match {
      case Some((tmstp, rnk)) if !isObsolete(tmstp) =>
        IO(rnk)
      case _ => for {
        rnk <- rankingService.rank(userId)
        _   <- IO { ranks.put(userId, (clock.now(), rnk)) }
      } yield rnk
    }

  override def score(userId: UserId): IO[ScoreBreakdown] =
    scores.get(userId) match {
      case Some((tmstp, scr)) if !isObsolete(tmstp) =>
        IO(scr)
      case _ => for {
        scr <- rankingService.score(userId)
        _   <- IO { scores.put(userId, (clock.now(), scr)) }
      } yield scr
    }

  private[this] def isObsolete(tmstp: UTCTimestamp) =
    tmstp.plusMillis(retention.toMillis).value <= clock.now().value

}
