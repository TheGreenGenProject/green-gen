package org.greengen.store.event

import org.greengen.core.Page
import org.greengen.core.event.{Event, EventId}
import org.greengen.core.user.UserId
import org.greengen.store.Store

trait EventStore[F[_]] extends Store[F] {

  def registerEvent(owner: UserId, eventId: EventId, event: Event): F[Unit]

  def cancelEvent(eventId: EventId): F[Unit]

  def exists(eventId: EventId): F[Boolean]

  def isEnabled(eventId: EventId): F[Boolean]

  def isParticipating(userId: UserId, eventId: EventId): F[Boolean]

  def isParticipationRequested(userId: UserId, eventId: EventId): F[Boolean]

  def getById(eventId: EventId): F[Option[Event]]

  def getByOwner(userId: UserId, page: Page): F[List[EventId]]

  def getByUser(userId: UserId, page: Page, filter: Event => Boolean): F[List[EventId]]

  def getByParticipation(userId: UserId, page: Page): F[List[EventId]]

  def getParticipationRequests(eventId: EventId, page: Page): F[List[UserId]]

  def participantCount(eventId: EventId): F[Long]

  def participants(eventId: EventId, page: Page): F[List[UserId]]

  def requestParticipation(user: UserId, event: EventId): F[Unit]

  def cancelParticipation(user: UserId, event: EventId): F[Unit]

  def removeParticipationRequest(user: UserId, event: EventId): F[Unit]

  def addParticipation(user: UserId, event: EventId): F[Unit]

}
