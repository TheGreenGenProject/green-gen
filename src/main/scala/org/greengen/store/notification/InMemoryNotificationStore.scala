package org.greengen.store.notification

import cats.effect.IO
import cats.implicits._
import org.greengen.core.{Page, PagedResult}
import org.greengen.core.notification.{Notification, NotificationId}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryNotificationStore extends NotificationStore[IO] {

  private[this] val notifications = new TrieMap[NotificationId, Notification]
  private[this] val dispatchQueues = new TrieMap[UserId, List[NotificationId]]

  override def register(id: NotificationId, value: Notification): IO[Unit] =
    IO(notifications.put(id, value))

  override def getNotification(id: NotificationId): IO[Option[Notification]] =
    IO(notifications.get(id))

  override def hasUnreadNotifications(userId: UserId): IO[Boolean] =
    IO(dispatchQueues.getOrElse(userId, List()).nonEmpty)

  override def getQueueForUser(userId: UserId, page: Page): IO[List[Notification]] = for {
    notifIds <- IO(dispatchQueues.getOrElse(userId, List()))
    notifs   <-  notifIds.map(getNotification).sequence
    paged    <- IO(PagedResult.page(notifs.flatten, page))
  } yield paged

  override def addToUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] =
    IO(dispatchQueues.updateWith(userId) {
        case Some(q) => Some(notificationId :: q)
        case None => Some(List(notificationId))
    })

  override def removeFromUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] =
    IO(dispatchQueues.updateWith(userId)(q => q.map(_.filterNot(_==notificationId))))
}
