package org.greengen.core.event

import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Location, Page, Schedule}

trait EventService[F[_]] {

  // Creates a new event
  def create(owner: UserId,
             maxParticipants: Int,
             description: String,
             location: Location,
             schedule: Schedule): F[Event]

  // Cancel an existing event
  def cancel(owner: UserId, id: EventId): F[Unit]

  def byId(id: EventId): F[Option[Event]]

  def byOwnership(id: UserId, page: Page): F[List[EventId]]

  def byUser(id: UserId, page: Page, filter: Event => Boolean): F[List[EventId]]

  def isParticipating(eventId: EventId, userId: UserId): F[Boolean]

  def isParticipationRequested(eventId: EventId, userId: UserId): F[Boolean]

  def isCancelled(eventId: EventId): F[Boolean]

  def byParticipation(id: UserId, page: Page): F[List[EventId]]

  def participants(eventId: EventId, page: Page): F[List[UserId]]

  def participantCount(eventId: EventId): F[Long]

  // Retrieve participation requests for a given event
  def participationRequests(event: EventId, page: Page): F[List[UserId]]

  // User is requesting to join an event
  def requestParticipation(id: UserId, event: EventId): F[Unit]

  // User is canceling its participation request / participation (in case it was accepted)
  def cancelParticipation(id: UserId, event: EventId): F[Unit]

  // Owner accepts participation
  def acceptParticipation(owner: UserId, participantId: UserId, event: EventId): F[Unit]

  // Owner rejects participation
  def rejectParticipation(owner: UserId, participantId: UserId, event: EventId): F[Unit]

}


object EventService {

  val AllEventsFilter = (_: Event) => true

  val IncomingFilter = (clock: Clock, e: Event) => !e.schedule.hasStarted(clock)

  val FinishedFilter = (clock: Clock, e: Event) => e.schedule.isOver(clock)

}