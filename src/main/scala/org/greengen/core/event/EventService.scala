package org.greengen.core.event

import org.greengen.core.user.UserId
import org.greengen.core.{Location, Schedule}

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

  def byIds(ids: EventId*): F[List[Event]]

  def byOwnership(id: UserId): F[List[EventId]]

  def byParticipation(id: UserId): F[List[EventId]]

  // Retrieve participation requests for a given event
  def participationRequests(event: EventId): F[List[UserId]]

  // User is requesting to join an event
  def requestParticipation(id: UserId, event: EventId): F[Unit]

  // Accept participation
  def acceptParticipation(owner: UserId, participantId: UserId, event: EventId): F[Unit]

  // Reject participation
  def rejectParticipation(owner: UserId, participantId: UserId, event: EventId): F[Unit]

}
