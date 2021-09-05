package org.greengen.http.event

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.{Clock, Page, UUID}
import org.greengen.core.event.{Event, EventId, EventService}
import org.greengen.core.event.EventService._
import org.greengen.core.user.UserId
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._


object HttpEventService {

  val PageSize = 10

  def routes(clock: Clock, service: EventService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "event" / "by-id" / UUIDVar(id) as _ =>
      service.byId(EventId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-owner" / UUIDVar(id) / IntVar(page) as _ =>
      service.byOwnership(UserId(UUID.from(id)), Page(page, by=PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-user" / "cancelled" / UUIDVar(id) / IntVar(page) as _ =>
      val cancelledFilter = (e: Event) => service.isCancelled(e.id).unsafeRunSync()
      service.byUser(UserId(UUID.from(id)), Page(page, by=PageSize), cancelledFilter)
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-user" / "incoming" / UUIDVar(id) / IntVar(page) as _ =>
      service.byUser(UserId(UUID.from(id)), Page(page, by=PageSize), IncomingFilter(clock, _))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-user" / "accepted" / UUIDVar(id) / IntVar(page) as _ =>
      val userId = UserId(UUID.from(id))
      val accepted = (e: Event) =>
        service.isParticipating(e.id, userId).unsafeRunSync()
      service.byUser(userId, Page(page, by=PageSize), accepted)
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-user" / "finished" / UUIDVar(id) / IntVar(page) as _ =>
      service.byUser(UserId(UUID.from(id)), Page(page, by=PageSize), FinishedFilter(clock, _))
        .flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-user" / "pending-request" / UUIDVar(id) / IntVar(page) as _ =>
      val userId = UserId(UUID.from(id))
      val pendingRequestFilter = (e: Event) =>
        service.isParticipationRequested(e.id, userId).unsafeRunSync()
      service.byUser(userId, Page(page, by=PageSize), pendingRequestFilter).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-user" / "confirmed" / UUIDVar(id) / IntVar(page) as _ =>
      val userId = UserId(UUID.from(id))
      val acceptedRequestFilter = (e: Event) =>
        service.isParticipating(e.id, userId).unsafeRunSync()
      service.byUser(userId, Page(page, by=PageSize), acceptedRequestFilter).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "is-participating" / UUIDVar(id) as userId =>
      service.isParticipating(EventId(UUID.from(id)), userId).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "participation"/ "is-requested" / UUIDVar(id) as userId =>
      service.isParticipationRequested(EventId(UUID.from(id)), userId).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "participation"/ "count" / UUIDVar(id) as _ =>
      service.participantCount(EventId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "is-cancelled" / UUIDVar(id) as _ =>
      service.isCancelled(EventId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "by-participation" / UUIDVar(id) / IntVar(page) as _ =>
      service.byParticipation(UserId(UUID.from(id)), Page(page, by=PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "event" / "participation" / "requests" / UUIDVar(id) / IntVar(page) as _ =>
      service.participationRequests(EventId(UUID.from(id)), Page(page, by=PageSize)).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "event" / "create" :?
      UserIdQueryParamMatcher(userId) +&
        MaxParticipantQueryParamMatcher(maxParticipants) +&
        DescriptionQueryParamMatcher(desc) +&
        LocationQueryParamMatcher(location) +&
        ScheduleQueryParamMatcher(schedule) as _ =>
      service.create(userId, maxParticipants, desc, location, schedule).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "cancel" :?
        EventIdQueryParamMatcher(eventId) as userId =>
      service.cancel(userId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "request" :?
        EventIdQueryParamMatcher(eventId) as userId =>
      service.requestParticipation(userId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "cancel" :?
        EventIdQueryParamMatcher(eventId) as userId =>
      service.cancelParticipation(userId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "accept" :?
        ParticpantIdQueryParamMatcher(participantId) +&
        EventIdQueryParamMatcher(eventId) as ownerId =>
      service.acceptParticipation(ownerId, participantId, eventId).flatMap(r => Ok(r.asJson))
    case POST -> Root / "event" / "participation" / "reject" :?
        ParticpantIdQueryParamMatcher(participantId) +&
        EventIdQueryParamMatcher(eventId) as ownerId =>
      service.rejectParticipation(ownerId, participantId, eventId).flatMap(r => Ok(r.asJson))
  }

}
