package org.greengen.impl.event

import cats.effect.IO
import cats.implicits._
import org.greengen.core.event.{Event, EventId, EventService}
import org.greengen.core.notification.{EventCancelledNotification, EventParticipationRequestAcceptedNotification, EventParticipationRequestRejectedNotification, Notification, NotificationService}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Location, Page, Schedule}
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
    event = Event(EventId.newId(), owner, maxParticipants, description, location, schedule)
    _ <- eventStore.registerEvent(event)
  } yield event

  override def cancel(owner: UserId, id: EventId): IO[Unit] = for {
    _ <- checkEvent(id)
    _ <- checkOwner(owner, id)
    _ <- eventStore.cancelEvent(id)
    requests <- eventStore.getParticipationRequests(id, Page.All)
    participants <- eventStore.participants(id, Page.All)
    notif = Notification.from(clock, EventCancelledNotification(id))
    _ <- notificationService.dispatch(notif, owner :: requests ::: participants)
    // Effectively canceling events
//    _ <- requests.map(eventStore.cancelParticipation(_, id)).sequence
//    _ <- participants.map(eventStore.cancelParticipation(_, id)).sequence
  } yield ()

  override def byId(id: EventId): IO[Option[Event]] =
    eventStore.getById(id)

  override def byOwnership(id: UserId, page: Page): IO[List[EventId]] =
    eventStore.getByOwner(id, page)

  override def byUser(id: UserId, page: Page, predicate: Event => Boolean): IO[List[EventId]] =
    eventStore.getByUser(id, page, predicate)

  override def byParticipation(id: UserId, page: Page): IO[List[EventId]] =
    eventStore.getByParticipation(id, page)

  override def isParticipating(eventId: EventId, userId: UserId): IO[Boolean] =
    eventStore.isParticipating(userId, eventId)

  override def isParticipationRequested(eventId: EventId, userId: UserId): IO[Boolean] =
    eventStore.isParticipationRequested(userId, eventId)

  override def isCancelled(eventId: EventId): IO[Boolean] =
    eventStore.isEnabled(eventId).map(! _)

  override def participants(eventId: EventId, page: Page): IO[List[UserId]] =
    eventStore.participants(eventId, page)

  override def participantCount(eventId: EventId): IO[Long] =
    eventStore.participantCount(eventId)

  override def participationRequests(event: EventId, page: Page): IO[List[UserId]] = for {
    _   <- checkEvent(event)
    res <- eventStore.getParticipationRequests(event, page)
  } yield res

  override def requestParticipation(user: UserId, event: EventId): IO[Unit] = for {
    _ <- checkUser(user)
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- eventStore.requestParticipation(user, event)
  } yield ()

  override def cancelParticipation(user: UserId, event: EventId): IO[Unit] = for {
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- eventStore.cancelParticipation(user, event)
  } yield ()

  override def acceptParticipation(owner: UserId, participant: UserId, event: EventId): IO[Unit] = for {
    _ <- checkOwner(owner, event)
    _ <- checkEvent(event)
    _ <- checkEventEnabled(event)
    _ <- checkNotParticipatingYet(participant, event)
    _ <- eventStore.addParticipation(participant, event)
    notif = Notification.from(clock, EventParticipationRequestAcceptedNotification(event))
    _ <- notificationService.dispatch(notif, List(participant))
  } yield ()

  override def rejectParticipation(owner: UserId, participant: UserId, event: EventId): IO[Unit] = for {
    _ <- checkOwner(owner, event)
    _ <- checkEvent(event)
    _ <- eventStore.removeParticipationRequest(participant, event)
    notif = Notification.from(clock, EventParticipationRequestRejectedNotification(event))
    _ <- notificationService.dispatch(notif, List(participant))
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
    notParticipating <- eventStore.isParticipating(participant, id).map(! _)
    _                <- IOUtils.check(notParticipating, s"User $participant is already participating to the event")
  } yield ()

  private[this] def checkMaxParticipants(max: Int): IO[Unit] =
    IOUtils.check(max > 0, s"Invalid max number of participants: $max")

  private[this] def checkSchedule(clock: Clock, schedule: Schedule): IO[Unit] =
    IOUtils.check(!schedule.isOver(clock), s"Invalid schedule $schedule")
}
