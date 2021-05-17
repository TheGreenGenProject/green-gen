package org.greengen.impl.event

import cats.effect.IO
import org.greengen.core.event.{Event, EventId, EventService}
import org.greengen.core.notification.NotificationService
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Location, Schedule}
import org.greengen.store.event.EventStore


class EventServiceImpl(eventStore: EventStore[IO])
                      (clock: Clock,
                       userService: UserService[IO],
                       notificationService: NotificationService[IO])
  extends EventService[IO] {

  override def create(owner: UserId,
                      maxParticipants: Int,
                      description: String,
                      location: Location,
                      schedule: Schedule): IO[Event] = for {
    _ <- checkUser(owner)
    _ <- checkMaxParticipants(maxParticipants)
    _ <- checkSchedule(clock, schedule)
    event = Event(EventId.newId(), owner, List(), maxParticipants, description, location, schedule, enabled = true)
    _ <- eventStore.registerEvent(owner, event.id, event)
  } yield event

  override def cancel(owner: UserId, id: EventId): IO[Unit] = for {
    _ <- checkEvent(id)
    _ <- checkOwner(owner, id)
    _ <- eventStore.cancelEvent(id)
    // FIXME automatically cancels request
    // FIXME notify participant and user with a request
  } yield ()

  override def byId(id: EventId): IO[Option[Event]] =
    eventStore.getById(id)

  override def byIds(ids: EventId*): IO[List[Event]] =
    eventStore.getByIds(ids.toList)

  override def byOwnership(id: UserId): IO[List[EventId]] =
    eventStore.getByOwner(id)

  override def byParticipation(id: UserId): IO[List[EventId]] =
    eventStore.getByParticipation(id)

  override def participationRequests(event: EventId): IO[List[UserId]] = for {
    _   <- checkEvent(event)
    res <- eventStore.getParticipationRequests(event)
  } yield res

  override def requestParticipation(user: UserId, event: EventId): IO[Unit] = for {
    _ <- checkUser(user)
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- eventStore.requestParticipation(user, event)
  } yield ()

  override def acceptParticipation(owner: UserId, participant: UserId, event: EventId): IO[Unit] = for {
    _ <- checkOwner(owner, event)
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- checkNotParticipatingYet(participant, event)
    _ <- eventStore.addParticipation(participant, event)
  } yield ()

  override def rejectParticipation(owner: UserId, participant: UserId, event: EventId): IO[Unit] = for {
    _ <- checkOwner(owner, event)
    _ <- checkEvent(event)
    _ <- eventStore.removeParticipationRequest(participant, event)
    _ <- notificationService.dispatch(???, List(participant))
  } yield ()


  // Checkers

  private[this] def checkEvent(id: EventId): IO[Unit] = for {
    exists <- eventStore.exists(id)
    _      <- IOUtils.check(exists, s"Unknown event $id")
  } yield ()


  private[this] def checkEventEnabled(id: EventId) = for {
    enabled <- eventStore.isEnabled(id)
    _       <- IOUtils.check(enabled, s"Event $id has been cancelled")
  } yield ()


  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkOwner(owner: UserId, id: EventId): IO[Unit] = for {
    event   <- eventStore.getById(id)
    isOwner <- IO(event.exists(_.owner==owner))
    _       <- IOUtils.check(isOwner, s"User $owner is not the owner of the event")
  } yield ()

  private[this] def checkNotParticipatingYet(participant: UserId, id: EventId): IO[Unit] = for {
    event            <- eventStore.getById(id)
    notParticipating <- IO(event.exists(!_.participants.contains(participant)))
    _                <- IOUtils.check(notParticipating, s"User $participant is not a participant of the event")
  } yield ()

  private[this] def checkMaxParticipants(max: Int): IO[Unit] =
    IOUtils.check(max > 0, s"Invalid max number of participants: $max")

  private[this] def checkSchedule(clock: Clock, schedule: Schedule): IO[Unit] =
    IOUtils.check(!schedule.isOver(clock), s"Invalid schedule $schedule")
}
