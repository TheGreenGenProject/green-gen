package org.greengen.store.notification

import org.greengen.core.Page
import org.greengen.core.notification.{Notification, NotificationId, NotificationWithReadStatus}
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait NotificationStore[F[_]] extends Store[F] {

  def register(id: NotificationId, value: Notification): F[Unit]

  def getNotification(id: NotificationId): F[Option[Notification]]

  def hasUnreadNotifications(userId: UserId): F[Boolean]

  def getQueueForUser(userId: UserId, page: Page, unreadOnly: Boolean): F[List[NotificationWithReadStatus]]

  def addToUserQueue(userId: UserId, notificationId: NotificationId): F[Unit]

  def removeFromUserQueue(userId: UserId, notificationId: NotificationId): F[Unit]

  def markAsRead(userId: UserId, notificationId: NotificationId): F[Unit]

}
