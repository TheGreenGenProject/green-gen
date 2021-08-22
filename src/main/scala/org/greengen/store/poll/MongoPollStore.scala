package org.greengen.store.poll

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Aggregates.{sortByCount, `match` => filter}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, set}
import org.greengen.core.poll._
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Page}
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase


class MongoPollStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends PollStore[IO] {

  import Conversions._
  import Schema._

  val PollCollection = "posts.polls"
  val PollAnswerCollection = "posts.polls.answers"

  val pollCollection = db.getCollection(PollCollection)
  val pollAnswerCollection = db.getCollection(PollAnswerCollection)

  override def register(poll: Poll): IO[Unit] = unitIO {
    pollCollection.insertOne(pollToDoc(poll))
  }

  override def getById(pollId: PollId): IO[Option[Poll]] = flattenFirstOptionIO {
    pollCollection
      .find(eql("poll_id", pollId.value.uuid))
      .limit(1)
      .map(docToPoll(_).toOption)
  }

  override def getByAuthor(author: UserId, page: Page): IO[List[PollId]] = toListIO {
    pollCollection
      .find(eql("author", author.value.uuid))
      .sort(descending("timestamp"))
      .paged(page)
      .map(asPollId(_))
  }

  override def isClosed(pollId: PollId): IO[Boolean] = firstOptionIO {
    pollCollection
      .find(eql("poll_id", pollId.value.uuid))
      .limit(1)
      .map(_.getBoolean("closed", false))
  }.map(_.getOrElse(false))

  override def close(pollId: PollId): IO[Unit] = unitIO {
    pollCollection
      .updateOne(eql("poll_id", pollId.value.uuid),
        combine(
          set("closed", true),
          set("closed_at", clock.now().value))
      )
  }

  override def answer(pollId: PollId, answer: PollAnswer): IO[Unit] =  unitIO {
    pollAnswerCollection.insertOne(pollAnswerToDoc(pollId, answer))
  }

  override def getAnswer(pollId: PollId, userId: UserId): IO[Option[PollAnswer]] = flattenFirstOptionIO {
    pollAnswerCollection
      .find(and(
        eql("poll_id", pollId.value.uuid),
        eql("user_id", userId.value.uuid)))
      .limit(1)
      .map(docToPollAnswer(_).toOption)
  }

  override def hasResponded(pollId: PollId, userId: UserId): IO[Boolean] =
    getAnswer(pollId, userId).map(_.isDefined)

  override def countRespondents(pollId: PollId): IO[Long] = firstIO {
    pollAnswerCollection
      .countDocuments(eql("poll_id", pollId.value.uuid))
  }

  override def getStatistics(pollId: PollId): IO[PollStats] = firstIO {
    pollAnswerCollection
      .aggregate(List(
        filter(eql("poll_id", pollId.value.uuid)),
        sortByCount("$option")))
      .collect()
      .map(_.toList)
      .map { docs =>
        docs.map(doc => PollStatsEntry(
          PollOption(doc.getString("_id")),
          doc.getInteger("count").toLong))
      }
      .map(stats => PollStats(pollId, stats))
  }

}
