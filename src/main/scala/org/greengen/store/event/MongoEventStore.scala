package org.greengen.store.event

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, in, eq => eql}
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, set, setOnInsert}
import org.greengen.core.event.{Event, EventId}
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Page}
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.UpdateOptions


class MongoEventStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO])
  extends EventStore[IO] {

  import Conversions._
  import Schema._

  val ParticipationCancelled = "Cancelled"
  val ParticipationAccepted = "Accepted"
  val ParticipationRequested = "Requested"

  val EventCollection = "posts.events"
  val EventStatusCollection = "posts.events.status"
  val EventParticipationCollection = "posts.events.participation"

  val eventCollection = db.getCollection(EventCollection)
  val eventStatusCollection = db.getCollection(EventStatusCollection)
  val eventParticiationCollection = db.getCollection(EventParticipationCollection)


  override def registerEvent(event: Event): IO[Unit] = unitIO {
    eventCollection.insertOne(eventToDoc(event))
  }

  override def cancelEvent(eventId: EventId): IO[Unit] = unitIO {
    eventStatusCollection
      .updateOne(
        eql("event_id", eventId.value.uuid),
        combine(
          setOnInsert("event_id", eventId.value.uuid),
          set("enabled", false),
        ),
        (new UpdateOptions).upsert(true)
      )
  }

  override def exists(eventId: EventId): IO[Boolean] = firstOptionIO {
    eventCollection
      .find(eql("event_id", eventId.value.uuid))
      .limit(1)
  }.map(_.isDefined)

  override def isEnabled(eventId: EventId): IO[Boolean] = firstOptionIO {
    eventStatusCollection
      .find(eql("event_id", eventId.value.uuid))
      .limit(1)
      .map(_.getBoolean("enabled", true))
  }.map(r => r.getOrElse(true))

  override def isParticipating(userId: UserId, eventId: EventId): IO[Boolean] = firstOptionIO {
    eventParticiationCollection
      .find(and(
        eql("event_id", eventId.value.uuid),
        eql("user_id", userId.value.uuid)
      ))
      .first()
      .map(_.getString("participation_status"))
  }.map(_.contains(ParticipationAccepted))

  override def isParticipationRequested(userId: UserId, eventId: EventId): IO[Boolean] = firstOptionIO {
    eventParticiationCollection
      .find(and(
        eql("event_id", eventId.value.uuid),
        eql("user_id", userId.value.uuid)
      ))
      .limit(1)
      .map(_.getString("participation_status"))
  }.map(_.contains(ParticipationRequested))

  override def getById(eventId: EventId): IO[Option[Event]] = flattenFirstOptionIO {
    eventCollection
      .find(eql("event_id", eventId.value.uuid))
      .limit(1)
      .map(docToEvent(_).toOption)
  }

  override def getByOwner(userId: UserId, page: Page): IO[List[EventId]] = toListIO {
    eventCollection
      .find(eql("owner", userId.value.uuid))
      .sort(descending("schedule.start"))
      .paged(page)
      .map(asEventId)
  }

  // FIXME to improve
  // Refactor needed to get rid of the predicate when possible
  override def getByUser(userId: UserId, page: Page, predicate: Event => Boolean): IO[List[EventId]] =
    for {
      eventIds <- toListIO {
        eventParticiationCollection
          .find(eql("user_id", userId.value.uuid))
          .map(asEventId(_))
          .map(_.value.uuid)
      }
      filtered <- toPagedListIO(page) {
        eventCollection
          .find(in("event_id", eventIds:_*))
          .map(docToEvent(_).toOption.filter(predicate).map(_.id))
          .filter(_.isDefined)
          .map(_.get)
      }
    } yield filtered

  override def getByParticipation(userId: UserId, page: Page): IO[List[EventId]] =
    getByParticipation(userId, ParticipationAccepted, page)

  override def getParticipationRequests(eventId: EventId, page: Page): IO[List[UserId]] = toListIO {
    eventParticiationCollection
      .find(and(
        eql("event_id", eventId.value.uuid),
        eql("participation_status", ParticipationRequested)
      )).sort(descending("timestamp"))
      .paged(page)
      .map(asUserId)
  }

  override def participantCount(eventId: EventId): IO[Long] = firstIO {
    eventParticiationCollection
      .countDocuments(and(
        eql("event_id", eventId.value.uuid),
        eql("participation_status", ParticipationAccepted)
      ))
  }

  override def participants(eventId: EventId, page: Page): IO[List[UserId]] = toListIO {
    eventParticiationCollection
      .find(and(
        eql("event_id", eventId.value.uuid),
        eql("participation_status", ParticipationAccepted)
      )).sort(descending("timestamp"))
      .paged(page)
      .map(asUserId)
  }

  override def requestParticipation(user: UserId, event: EventId): IO[Unit] = unitIO {
    eventParticiationCollection
      .updateOne(
        and(
          eql("event_id", event.value.uuid),
          eql("user_id", user.value.uuid)
        ),
        combine(
          setOnInsert("event_id", event.value.uuid),
          setOnInsert("user_id", user.value.uuid),
          set("participation_status", ParticipationRequested),
          set("timestamp", clock.now().value)
        ),
        (new UpdateOptions).upsert(true)
      )
  }

  override def cancelParticipation(user: UserId, event: EventId): IO[Unit] = unitIO {
    eventParticiationCollection
      .updateOne(
        and(
          eql("event_id", event.value.uuid),
          eql("user_id", user.value.uuid)
        ),
        combine(
          set("participation_status", ParticipationCancelled),
          set("timestamp", clock.now().value)
        )
      )
  }

  override def removeParticipationRequest(user: UserId, event: EventId): IO[Unit] = unitIO {
    eventParticiationCollection
      .updateOne(
        and(
          eql("event_id", event.value.uuid),
          eql("user_id", user.value.uuid)
        ),
        combine(
          set("participation_status", ParticipationCancelled),
          set("timestamp", clock.now().value)
        )
      )
  }

  override def addParticipation(user: UserId, event: EventId): IO[Unit] = unitIO {
    eventParticiationCollection
      .updateOne(
        and(
          eql("event_id", event.value.uuid),
          eql("user_id", user.value.uuid)
        ),
        combine(
          set("participation_status", ParticipationAccepted),
          set("timestamp", clock.now().value)
        )
      )
  }

  // Helpers

  private[this] def getByParticipation(userId: UserId, status: String, page: Page): IO[List[EventId]] = toListIO {
    eventParticiationCollection
      .find(and(
        eql("user_id", userId.value.uuid),
        in("participation_status", status)
      )).sort(descending("timestamp"))
      .paged(page)
      .map(asEventId)
  }

}
