package org.greengen.store.event

import cats.effect.IO
import org.greengen.core.event.{Event, EventId}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

class InMemoryEventStore extends EventStore[IO] {

  private[this] val events = new TrieMap[EventId, Event]()
  private[this] val eventsByOwner = new TrieMap[UserId, List[EventId]]()
  private[this] val eventsByParticipants = new TrieMap[UserId, List[EventId]]()
  private[this] val participationRequestsByUser = new TrieMap[UserId, Set[EventId]]()
  private[this] val participationRequestsByEvent = new TrieMap[EventId, Set[UserId]]()


  override def registerEvent(owner: UserId, eventId: EventId, event: Event): IO[Unit] = IO {
    events.put(eventId, event)
    eventsByOwner.updateWith(owner) {
      case Some(events) => Some(eventId :: events)
      case None => Some(List(eventId))
    }
  }

  override def cancelEvent(eventId: EventId): IO[Unit] = IO {
    events.updateWith(eventId) {
      case Some(evt) => Some(evt.copy(enabled = false))
      case None => None
    }
  }


  override def exists(eventId: EventId): IO[Boolean] =
    IO(events.contains(eventId))

  override def isEnabled(eventId: EventId): IO[Boolean] =
    IO(events.get(eventId).map(_.enabled).getOrElse(false))

  override def getById(eventId: EventId): IO[Option[Event]] =
    IO(events.get(eventId))

  override def getByIds(ids: List[EventId]): IO[List[Event]] =
    IO(ids.map(events.get(_)).flatten)

  override def getByOwner(userId: UserId): IO[List[EventId]] =
    IO(eventsByOwner.getOrElse(userId, List()))

  override def getByParticipation(userId: UserId): IO[List[EventId]] =
    IO(eventsByParticipants.getOrElse(userId, List()))

  override def getParticipationRequests(eventId: EventId): IO[List[UserId]] =
    IO(participationRequestsByEvent.getOrElse(eventId, Set()).toList)

  override def requestParticipation(user: UserId, event: EventId): IO[Unit] = IO {
    participationRequestsByUser.updateWith(user) {
      case Some(events) => Some(events + event)
      case None => Some(Set(event))
    }
    participationRequestsByEvent.updateWith(event) {
      case Some(users) => Some(users + user)
      case None => Some(Set(user))
    }
  }

  override def removeParticipationRequest(user: UserId, event: EventId): IO[Unit] = IO { events.updateWith(event) {
    case Some(evt) => Some(evt.copy(participants = evt.participants.filterNot(_==user)))
    case None => None
  }}

  override def addParticipation(participant: UserId, event: EventId): IO[Unit] = IO {
    events.updateWith(event) {
    case Some(evt) => Some(evt.copy(participants = participant :: evt.participants))
    case None => None
  }}

}
