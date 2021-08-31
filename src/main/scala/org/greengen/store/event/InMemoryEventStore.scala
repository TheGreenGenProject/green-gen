package org.greengen.store.event

import cats.effect.IO
import org.greengen.core.{Clock, Page, PagedResult, UTCTimestamp}
import org.greengen.core.event.{Event, EventId}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryEventStore(clock: Clock) extends EventStore[IO] {

  private[this] val events = new TrieMap[EventId, Event]()
  private[this] val disabledEvents = new TrieMap[EventId, UTCTimestamp]()
  private[this] val eventsByOwner = new TrieMap[UserId, List[EventId]]()
  private[this] val eventsByParticipant = new TrieMap[UserId, Set[EventId]]()
  private[this] val participantsByEvent = new TrieMap[EventId, Set[UserId]]()
  private[this] val participationRequestsByUser = new TrieMap[UserId, Set[EventId]]()
  private[this] val participationRequestsByEvent = new TrieMap[EventId, Set[UserId]]()


  override def registerEvent(owner: UserId, eventId: EventId, event: Event): IO[Unit] = IO {
    events.put(eventId, event)
    eventsByOwner.updateWith(owner) {
      case Some(events) => Some(eventId :: events)
      case None => Some(List(eventId))
    }
  }

  override def cancelEvent(eventId: EventId): IO[Unit] =
    IO(disabledEvents.put(eventId, clock.now()))

  override def exists(eventId: EventId): IO[Boolean] =
    IO(events.contains(eventId))

  override def isEnabled(eventId: EventId): IO[Boolean] =
    IO(!disabledEvents.contains(eventId))

  override def isParticipating(userId: UserId, eventId: EventId): IO[Boolean] =
    IO(participantsByEvent.getOrElse(eventId, Set()).contains(userId))

  override def isParticipationRequested(userId: UserId, eventId: EventId): IO[Boolean] =
    IO(participationRequestsByUser.getOrElse(userId, Set()).contains(eventId))

  override def getById(eventId: EventId): IO[Option[Event]] =
    IO(events.get(eventId))

  override def getByOwner(userId: UserId, page: Page): IO[List[EventId]] =
    IO(eventsByOwner.get(userId).map(PagedResult.page(_, page)).getOrElse(List()))

  override def getByParticipation(userId: UserId, page: Page): IO[List[EventId]] = for {
    requests <- IO(eventsByParticipant.getOrElse(userId, Set()))
    sorted   <- IO(requests.toList.sortBy(_.value.uuid))
    paged    <- IO(PagedResult.page(sorted, page))
  } yield paged

  override def participantCount(eventId: EventId): IO[Long] =
    IO(participantsByEvent.getOrElse(eventId, Set()).size)

  override def participants(eventId: EventId, page: Page): IO[List[UserId]] = for {
    requests <- IO(participantsByEvent.getOrElse(eventId, Set()))
    sorted   <- IO(requests.toList.sortBy(_.value.uuid))
    paged    <- IO(PagedResult.page(sorted, page))
  } yield paged

  override def getParticipationRequests(eventId: EventId, page: Page): IO[List[UserId]] = for {
    requests <- IO(participationRequestsByEvent.getOrElse(eventId, Set()))
    sorted   <- IO(requests.toList.sortBy(_.value.uuid))
    paged    <- IO(PagedResult.page(sorted, page))
  } yield paged

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

  override def cancelParticipation(user: UserId, event: EventId): IO[Unit] = for {
    _ <- removeParticipationRequest(user, event)
    _ <- IO { participantsByEvent.updateWith(event) {
      case Some(users) => Some(users - user)
      case None        => None
    }}
    _ <- IO { eventsByParticipant.updateWith(user) {
      case Some(events) => Some(events - event)
      case None        => None
    }}
  } yield ()

  override def removeParticipationRequest(user: UserId, event: EventId): IO[Unit] = for {
    // Removing from participation request indices
    _ <- IO { participationRequestsByEvent.updateWith(event) {
      case Some(users) => Some(users - user)
      case None        => None
    }}
    _ <- IO { participationRequestsByUser.updateWith(user) {
      case Some(events) => Some(events - event)
      case None        => None
    }}
  } yield ()

  override def addParticipation(participant: UserId, event: EventId): IO[Unit] = for {
    // Adding to the list of participants
    _ <- IO { participantsByEvent.updateWith(event) {
      case Some(users) => Some(users + participant)
      case None        => Some(Set(participant))
    }}
    // Removing from participation request indices
    _ <- removeParticipationRequest(participant, event)
  } yield ()

}
