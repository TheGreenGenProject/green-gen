package org.greengen.http.notification

import cats.effect.IO
import io.circe.generic.auto._
import org.greengen.core.notification.NotificationContent
import org.greengen.core.user.UserId
import org.http4s.circe.jsonOf

object JsonRequests {

  case class DispatchNotificationRequest(content: NotificationContent, users: List[UserId])
  implicit val DispatchNotificationRequestDecoder = jsonOf[IO, DispatchNotificationRequest]

}
