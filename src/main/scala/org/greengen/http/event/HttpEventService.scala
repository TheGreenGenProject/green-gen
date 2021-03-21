package org.greengen.http.event

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.UUID
import org.greengen.core.event.{EventId, EventService}
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpEventService {


  def routes(service: EventService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "event" / "by-id" / UUIDVar(id) as _ =>
      service.byId(EventId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-ids" / ids as _ =>
      val eventIds = ids.split('+').map(uuid => EventId(UUID.unsafeFrom(uuid))).toSeq
      service.byIds(eventIds: _*).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-owner" / UUIDVar(id) as _ =>
      service.byOwnership(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-participation" / UUIDVar(id) as _ =>
      service.byParticipation(UserId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "participation" / "requests" / UUIDVar(id) as _ =>
      service.participationRequests(EventId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "event" / "create" :?
      UserIdQueryParamMatcher(userId) +&
        MaxParticipantQueryParamMatcher(maxParticipants) +&
        DescriptionQueryParamMatcher(desc) +&
        LocationQueryParamMatcher(location) +&
        ScheduleQueryParamMatcher(schedule) as _ =>
      service.create(userId, maxParticipants, desc, location, schedule).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "cancel" :?
      OwnerIdQueryParamMatcher(ownerId) +&
        EventIdQueryParamMatcher(eventId) as _ =>
      service.cancel(ownerId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "request" :?
      UserIdQueryParamMatcher(userId) +&
        EventIdQueryParamMatcher(eventId) as _ =>
      service.requestParticipation(userId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "accept" :?
      OwnerIdQueryParamMatcher(ownerId) +&
        ParticpantIdQueryParamMatcher(participantId) +&
        EventIdQueryParamMatcher(eventId) as _ =>
      service.acceptParticipation(ownerId, participantId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "reject" :?
      OwnerIdQueryParamMatcher(ownerId) +&
        ParticpantIdQueryParamMatcher(participantId) +&
        EventIdQueryParamMatcher(eventId) as _ =>
      service.rejectParticipation(ownerId, participantId, eventId).flatMap(r => Ok(r.asJson))
  }

}
