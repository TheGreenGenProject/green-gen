package org.greengen.store.post

import java.util.concurrent.atomic.AtomicReference

import cats.Monad
import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Aggregates.{sortByCount, unwind, limit}
import com.mongodb.client.model.Filters.{all, eq => eql}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.post.{Post, PostId}
import org.greengen.core.user.UserId
import org.greengen.core.{Duration, Hashtag, Reason, UTCTimestamp, UUID}
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document


class MongoPostStore(db: MongoDatabase)
                    (implicit cs: ContextShift[IO]) extends PostStore[IO] {

  import Conversions._
  import Schema._

  val PostsCollection = "posts"
  val FlaggedPostsCollection = "posts.flagged"

  private[this] val postsCollection = db.getCollection(PostsCollection)
  private[this] val flaggedCollection = db.getCollection(FlaggedPostsCollection)

  // Cache containing the last calculated status of the hashtag trend
  val trendCalculationValidity = Duration(30 * 60 * 1000) // Recompute trend every 30 minutes
  private[this] val trendCache =
    new AtomicReference[(UTCTimestamp, List[(Int, Hashtag)])]((UTCTimestamp(0L),List()))

  override def registerPost(post: Post): IO[Unit] = unitIO {
    postsCollection.insertOne(postToDoc(post))
  }

  override def exists(postId: PostId): IO[Boolean] = firstOptionIO {
    postsCollection
      .find(eql("post_id", postId.value.uuid))
      .limit(1)
  }.map(_.isDefined)

  override def getPostById(postId: PostId): IO[Option[Post]] = firstIO {
    postsCollection
      .find(eql("post_id", postId.value.uuid))
      .limit(1)
      .map(docToPost)
      .map(_.toOption)
  }

  override def getByAuthor(author: UserId): IO[Set[PostId]] = toSetIO {
    postsCollection
      .find(eql("author", author.value.uuid))
      .map(_.getString("post_id"))
      .map(UUID.unsafeFrom)
      .map(PostId(_))
  }

  override def getByChallengeId(challengeId: ChallengeId): IO[Option[PostId]] = firstOptionIO {
    postsCollection
      .find(eql("challenge_id", challengeId.value.uuid))
      .limit(1)
      .map(_.getString("post_id"))
      .map(UUID.unsafeFrom)
      .map(PostId(_))
  }

  override def getByHashtags(tags: Set[Hashtag]): IO[Set[PostId]] = toSetIO {
    postsCollection
      .find(all("hashtags", tags.map(_.value).toArray:_*))
      .map(_.getString("post_id"))
      .map(UUID.unsafeFrom)
      .map(PostId(_))
  }

  override def flagPost(flaggedBy: UserId,
                        post: PostId,
                        reason: Reason,
                        timestamp: UTCTimestamp): IO[Unit] = unitIO {
    flaggedCollection.insertOne(Document(
      "flagged_by" -> flaggedBy.value.uuid,
      "post"       -> post.value.uuid,
      "reason"     -> Reason.format(reason),
      "timestamp"  -> timestamp.value
    ))
  }

  override def isPostFlagged(post: PostId): IO[Boolean] = firstOptionIO {
    flaggedCollection
      .find(eql("post_id", post.value.uuid))
      .limit(1)
  }.map(_.isDefined)

  override def trendByHashtags(n: Int): IO[List[(Int, Hashtag)]] = for {
    cached <- getCachedTrend()
    now    <- now()
    trend  <- Monad[IO]
      .ifM(IO(cached._1.value < now.value - trendCalculationValidity.millis))(computeTrend(n), IO(cached._2))
    _      <- cacheTrend(trend)
  } yield trend

  // Helpers

  // Mongo query would be
  // db.posts.aggregate([
  //  {$unwind:"$hashtags"},
  //  {$group: {_id:"$hashtags", count:{$sum:1}}},
  //  {$sort: {"count": -1,"_id":1}}]
  // )
  private[this] def computeTrend(n: Int): IO[List[(Int, Hashtag)]] =  firstIO {
    postsCollection
      .aggregate(List(unwind("$hashtags"), sortByCount("$hashtags"), limit(n)))
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