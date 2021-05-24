package org.greengen.store.hashtag

import java.util.concurrent.atomic.AtomicReference

import cats.Monad
import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Aggregates.{limit, sortByCount}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.{combine, setOnInsert}
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Duration, Hashtag, UTCTimestamp}
import org.greengen.db.mongo.Conversions
import org.mongodb.scala.MongoDatabase



class MongoHashtagStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends HashtagStore[IO] {

  import Conversions._

  val HashtagCollection = "hashtags"
  val hashtagCollection = db.getCollection(HashtagCollection)

  // Cache containing the last calculated status of the hashtag trend
  val trendCalculationValidity = Duration(30 * 60 * 1000) // Recompute trend every 30 minutes
  private[this] val trendCache =
    new AtomicReference[(UTCTimestamp, List[(Int, Hashtag)])]((UTCTimestamp(0L),List()))


  override def getFollowers(ht: Hashtag): IO[Set[UserId]] = toSetIO {
    hashtagCollection
      .find(eql("hashtag", ht.value))
      .map(asUserId(_))
  }

  override def countFollowers(ht: Hashtag): IO[Long] = firstIO {
    hashtagCollection
      .countDocuments(eql("hashtag", ht.value))
  }

  override def addHashtagFollower(userId: UserId, ht: Hashtag): IO[Unit] = unitIO {
    hashtagCollection
      .updateOne(
        and(eql("hashtag", ht.value),eql("user_id", userId.value.uuid)),
        combine(
          setOnInsert("hashtag", ht.value),
          setOnInsert("user_id", userId.value.uuid),
          setOnInsert("timestamp", clock.now().value)
        ),
        (new UpdateOptions).upsert(true)
      )
  }

  override def removeHashtagFollower(userId: UserId, ht: Hashtag): IO[Unit] = unitIO {
    hashtagCollection
      .deleteOne(and(eql("hashtag", ht.value),eql("user_id", userId.value.uuid)))
  }

  override def hashtagsfollowedByUser(userId: UserId): IO[Set[Hashtag]] = toSetIO {
    hashtagCollection
      .find(eql("user_id", userId.value.uuid))
      .map(_.getString("hashtag"))
      .map(Hashtag(_))
  }

  override def trendByFollowers(n: Int): IO[List[(Int, Hashtag)]] = for {
    cached <- getCachedTrend()
    now    <- now()
    trend  <- Monad[IO]
      .ifM(IO(cached._1.value < now.value - trendCalculationValidity.millis))(computeTrend(n), IO(cached._2))
    _      <- cacheTrend(trend)
  } yield trend


  // Helpers

  private[this] def computeTrend(n: Int): IO[List[(Int, Hashtag)]] =  firstIO {
    hashtagCollection
      .aggregate(List(sortByCount("$hashtags"), limit(n)))
      .collect()
      .map(_.toList)
      .map { docs =>
        docs.map(doc => (doc.getInteger("count").toInt, Hashtag(doc.getString("_id"))))
      }
  }

  private[this] def cacheTrend(trend: List[(Int, Hashtag)]): IO[Unit] = IO {
    val now = System.currentTimeMillis()
    trendCache.set((UTCTimestamp(now), trend))
  }

  private[this] def getCachedTrend(): IO[(UTCTimestamp, List[(Int, Hashtag)])] =
    IO(trendCache.get)

  private[this] def now(): IO[UTCTimestamp] =
    IO(UTCTimestamp(System.currentTimeMillis()))
}
