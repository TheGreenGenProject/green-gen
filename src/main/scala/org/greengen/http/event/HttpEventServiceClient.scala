package org.greengen.http.event

import cats.effect.IO
import org.greengen.core.event.{Event, EventId, EventService}
import org.greengen.core.user.UserId
import org.greengen.core.{Location, Schedule}
import org.greengen.http.HttpHelper.post
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client.Client


class HttpEventServiceClient(httpClient: Client[IO], root: Uri) extends EventService[IO] {

  override def create(owner: UserId,
                      maxParticipants: Int,
                      description: String,
                      location: Location,
                      schedule: Schedule): IO[Event] =
    httpClient.expect[Event](post(root / "event" / "create",
      "user-id" -> owner.value.uuid,
      "max-participant" -> maxParticipants,
      "description" -> description,
      "location" -> ???,
      "schedule" -> ???))

  override def cancel(owner: UserId, id: EventId): IO[Unit] =
    httpClient.expect[Unit](post(root / "event" / "cancel",
      "owner-id" -> owner.value.uuid,
      "event-id" -> id.value.uuid))

  override def byId(id: EventId): IO[Option[Event]] =
    httpClient.expect[Option[Event]](root / "event" / "by-id" / id.value.uuid)

  override def byIds(ids: EventId*): IO[List[Event]] =
    httpClient.expect[List[Event]](root / "event" / "by-ids" / ids.map(_.value.uuid).mkString("+"))

  override def byOwnership(id: UserId): IO[List[EventId]] =
    httpClient.expect[List[EventId]](root / "event" / "by-owner" / id.value.uuid)

  override def byParticipation(id: UserId): IO[List[EventId]] =
    httpClient.expect[List[EventId]](root / "event" / "by-participation" / id.value.uuid)

  override def participationRequests(event: EventId): IO[List[UserId]] =
    httpClient.expect[List[UserId]](root / "event" / "participation" / "requests" / event.value.uuid)

  override def requestParticipation(userId: UserId, eventId: EventId): IO[Unit] =
    httpClient.expect[Unit](post(root / "event" / "participation" / "request",
      "user-id" -> userId.value.uuid,
      "event-id" -> eventId.value.uuid))

  override def acceptParticipation(owner: UserId, participantId: UserId, event: EventId): IO[Unit] =
    httpClient.expect[Unit](post(root / "event" / "participation" / "accept",
      "owner-id" -> owner.value.uuid,
      "participant-id" -> participantId.value.uuid,
      "event-id" -> event.value.uuid))

  override def rejectParticipation(owner: UserId, participantId: UserId, event: EventId): IO[Unit] =
    httpClient.expect[Unit](post(root / "event" / "participation" / "reject",
      "owner-id" -> owner.value.uuid,
      "participant-id" -> participantId.value.uuid,
      "event-id" -> event.value.uuid))
}
