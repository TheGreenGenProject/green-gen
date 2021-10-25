package org.greengen.http.notification

import cats.effect._
import org.greengen.core.{Clock, Page, UUID}
import org.greengen.core.notification.{Notification, NotificationId, NotificationService}
import org.greengen.core.user.UserId
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.greengen.http.notification.JsonRequests._
import org.greengen.http.HttpQueryParameters._

object HttpNotificationService {

  val PageSize = 50

  def routes(clock: Clock, service: NotificationService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "notification" / "by-id" / UUIDVar(id) as _ =>
      service.byId(NotificationId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "notification" / "all" / IntVar(page) as userId =>
      service.byUser(userId, Page(page, PageSize), unreadOnly = false).flatMap(r => Ok(r.asJson))
    case GET -> Root / "notification" / "all" / "unread" /IntVar(page) as userId =>
      service.byUser(userId, Page(page, PageSize), unreadOnly = true).flatMap(r => Ok(r.asJson))
    case GET -> Root / "notification" / "has-some" as userId =>
      service.hasUnreadNotification(userId).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "notification" / "read" :?
      NotificationIdQueryParamMatcher(notifId) as userId =>
      service.markAsRead(userId, notifId).flatMap(r => Ok(r.asJson))
    case req @ POST -> Root / "notification" / "dispatch" as _ =>
      for {
        r <- req.req.as[DispatchNotificationRequest]
        notif = Notification(NotificationId.newId, r.content, clock.now())
        dispatched <- service.dispatch(notif, r.users)
        res <- Ok(dispatched.asJson)
      } yield res
  }

}
