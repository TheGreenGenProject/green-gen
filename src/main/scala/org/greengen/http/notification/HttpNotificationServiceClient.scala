package org.greengen.http.notification

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.notification.{Notification, NotificationId, NotificationService}
import org.greengen.core.user.UserId
import org.greengen.http.HttpHelper._
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client._


class HttpNotificationServiceClient(httpClient: Client[IO], root: Uri) extends NotificationService[IO] {

  override def hasUnreadNotification(userId: UserId): IO[Boolean] =
    httpClient.expect[Boolean](root / "notification" / "has-some") // FIXME

  override def byId(id: NotificationId): IO[Option[Notification]] =
    httpClient.expect[Option[Notification]](root / "notification" / "by-id" / id.id.uuid)

  override def dispatch(notif: Notification, users: List[UserId]): IO[Unit] =
    httpClient.expect[Unit](post(root / "notification" / "dispatch")) // FIXME

  override def byUser(id: UserId, page: Page, unreadOnly: Boolean): IO[List[Notification]] =
    httpClient.expect[List[Notification]](root / "notification" / "all" / "unread" / page.n.toString) // FIXME

  override def markAsRead(userId: UserId, nid: NotificationId): IO[Unit] =
    httpClient.expect[Unit](post(root / "notification" / "read", "user-id" -> userId.value.uuid, "notif-id" -> nid))

  override def platform(message: String): IO[Unit] =
    httpClient.expect[Unit](post(root / "notification" / "platform", "message" ->message))

}
