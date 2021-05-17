package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.event.{Event, EventId, EventService}
import org.greengen.core.notification.NotificationService
import org.greengen.core.reminder.ReminderService
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Location, Schedule}

import scala.collection.concurrent.TrieMap


@deprecated
class InMemoryEventService(clock: Clock,
                           userService: UserService[IO],
                           notificationService: NotificationService[IO])
  extends EventService[IO] {

  private[this] val events = new TrieMap[EventId, Event]()
  private[this] val eventsByOwner = new TrieMap[UserId, List[EventId]]()
  private[this] val eventsByParticipants = new TrieMap[UserId, List[EventId]]()
  private[this] val participationRequestsByUser = new TrieMap[UserId, Set[EventId]]()
  private[this] val participationRequestsByEvent = new TrieMap[EventId, Set[UserId]]()


  override def create(owner: UserId,
                      maxParticipants: Int,
                      description: String,
                      location: Location,
                      schedule: Schedule): IO[Event] = for {
    _ <- checkUser(owner)
    _ <- checkMaxParticipants(maxParticipants)
    _ <- checkSchedule(clock, schedule)
    event = Event(EventId.newId(), owner, List(), maxParticipants, description, location, schedule, enabled = true)
    _ <- IO(registerEvent(owner, event.id, event))
  } yield event

  override def cancel(owner: UserId, id: EventId): IO[Unit] = for {
    _ <- checkEvent(id)
    _ <- checkOwner(owner, id)
    _ <- IO(cancelEvent(id))
    // FIXME automatically cancels request
  } yield ()

  override def byId(id: EventId): IO[Option[Event]] = 
    IO(events.get(id))

  override def byIds(ids: EventId*): IO[List[Event]] = IO {
    ids.map(events.get(_)).flatten.toList
  }

  override def byOwnership(id: UserId): IO[List[EventId]] =
    IO(eventsByOwner.getOrElse(id, List()))

  override def byParticipation(id: UserId): IO[List[EventId]] =
    IO(eventsByParticipants.getOrElse(id, List()))

  override def participationRequests(event: EventId): IO[List[UserId]] = for {
    _ <- checkEvent(event)
  } yield participationRequestsByEvent.getOrElse(event, Set()).toList

  override def requestParticipation(user: UserId, event: EventId): IO[Unit] = for {
    _ <- checkUser(user)
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- IO {
      participationRequestsByUser.updateWith(user) {
        case Some(events) => Some(events + event)
        case None => Some(Set(event))
      }
      participationRequestsByEvent.updateWith(event) {
        case Some(users) => Some(users + user)
        case None => Some(Set(user))
      }
    }
  } yield ()

  override def acceptParticipation(owner: UserId, participant: UserId, event: EventId): IO[Unit] = for {
    _ <- checkOwner(owner, event)
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- checkNotParticipatingYet(participant, event)
    _ <- IO { events.updateWith(event) {
      case Some(evt) => Some(evt.copy(participants = participant :: evt.participants))
      case None => None
    }}
  } yield ()

  override def rejectParticipation(owner: UserId, participant: UserId, event: EventId): IO[Unit] = for {
    _ <- checkOwner(owner, event)
    _ <- checkEvent(event)
    _ <- notificationService.dispatch(???, List(participant))
  } yield ()


  // Helpers

  private[this] def registerEvent(owner: UserId, eventId: EventId, event: Event): Unit = {
    events.put(eventId, event)
    eventsByOwner.updateWith(owner) {
      case Some(events) => Some(eventId :: events)
      case None => Some(List(eventId))
    }
  }

  private[this] def cancelEvent(eventId: EventId): Unit =
    events.updateWith(eventId) {
      case Some(evt) => Some(evt.copy(enabled = false))
      case None => None
    }

  // Checkers

  private[this] def checkEvent(id: EventId) =
    IOUtils.check(events.contains(id),
      s"Unknown event $id")

  private[this] def checkEventEnabled(id: EventId) =
    IOUtils.check(events.get(id).exists(_.enabled),
      s"Event $id has been cancelled")

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkOwner(owner: UserId, id: EventId) =
    IOUtils.check(events.get(id).exists(_.owner==owner),
      s"User $owner is not the owner of the event")

  private[this] def checkNotParticipatingYet(participant: UserId, id: EventId) =
    IOUtils.check(events.get(id).exists(!_.participants.contains(participant)),
      s"User $participant is not a participant of the event")

  private[this] def checkMaxParticipants(max: Int) =
    IOUtils.check(max > 0,
      s"Invalid max number of participants: $max")

  private[this] def checkSchedule(clock: Clock, schedule: Schedule) =
    IOUtils.check(!schedule.isOver(clock),
      s"Invalid schedule $schedule")

}
