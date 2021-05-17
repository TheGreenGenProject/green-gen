package org.greengen.store.notification

import org.greengen.core.notification.{Notification, NotificationId}
import org.greengen.core.user.UserId
import org.greengen.store.Store

trait NotificationStore[F[_]] extends Store[F] {

  def register(id: NotificationId, value: Notification): F[Unit]

  def getNotification(id: NotificationId): F[Option[Notification]]

  def getQueueForUser(userId: UserId): F[List[Notification]]

  def addToUserQueue(userId: UserId, notificationId: NotificationId): F[Unit]

  def removeFromUserQueue(userId: UserId, notificationId: NotificationId): F[Unit]

}
