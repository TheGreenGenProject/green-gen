package org.greengen.store.notification

import cats.effect.IO
import cats.implicits._
import org.greengen.core.{Clock, Page, PagedResult}
import org.greengen.core.notification.{Notification, NotificationId, NotificationWithReadStatus, Read, ReadStatus, Unread}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryNotificationStore(clock: Clock) extends NotificationStore[IO] {

  private[this] val notifications = new TrieMap[NotificationId, Notification]
  private[this] val dispatchQueues = new TrieMap[UserId, List[(NotificationId, ReadStatus)]]

  override def register(id: NotificationId, value: Notification): IO[Unit] =
    IO(notifications.put(id, value))

  override def getNotification(id: NotificationId): IO[Option[Notification]] =
    IO(notifications.get(id))

  override def hasUnreadNotifications(userId: UserId): IO[Boolean] =
    IO(dispatchQueues.getOrElse(userId, List()).find(_._2==Unread).isDefined)

  override def getQueueForUser(userId: UserId, page: Page, unreadOnly: Boolean): IO[List[NotificationWithReadStatus]] = for {
    notifIds <- IO(dispatchQueues.getOrElse(userId, List()))
    notifs   <-  notifIds.collect { case (id, status) if !unreadOnly || (unreadOnly && status==Unread) =>
      getNotification(id).map(notif => notif.map(NotificationWithReadStatus(_, status)))
    }.sequence
    paged    <- IO(PagedResult.page(notifs.flatten, page))
  } yield paged

  override def addToUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] =
    IO(dispatchQueues.updateWith(userId) {
        case Some(q) => Some((notificationId, Unread) :: q)
        case None => Some(List((notificationId, Unread)))
    })

  override def removeFromUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] =
    IO(dispatchQueues.updateWith(userId)(q => q.map(_.filterNot(_._1==notificationId))))

  override def markAsRead(userId: UserId, notificationId: NotificationId): IO[Unit] =
    IO(dispatchQueues.updateWith(userId) { q =>
      q.map(_.map { case (id, status) =>
        if(id==notificationId) (id, Read(clock.now())) else (id, status)
      })
    })
}
