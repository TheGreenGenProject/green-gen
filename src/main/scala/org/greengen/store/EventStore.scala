package org.greengen.store

import org.greengen.core.UUID
import org.greengen.core.event.Event

trait EventStore[F[_]] {

  def newEvent(): F[Event]

  def addParticipant(eventId: UUID, participant: UUID): F[Boolean]

  def updateEvent(event: Event): F[Boolean]

  def byId(id: UUID): F[Option[Event]]

}
