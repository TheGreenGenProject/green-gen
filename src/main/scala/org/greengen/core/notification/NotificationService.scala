package org.greengen.core.notification

import org.greengen.core.Page
import org.greengen.core.user.UserId

trait NotificationService[F[_]] {

  // True if there is unread notifications for the given user
  def hasUnreadNotification(userId: UserId): F[Boolean]

  def byId(id: NotificationId): F[Option[Notification]]

  // Dispatch a notification to a list of users
  def dispatch(notif: Notification, users: List[UserId]): F[Unit]

  // Retrieve notifications for a given user id
  def byUser(id: UserId, page: Page, unreadOnly: Boolean): F[List[NotificationWithReadStatus]]

  // Mark a notification as read
  def markAsRead(id: UserId, nid: NotificationId): F[Unit]

  // Dispatch a platform notification to all users
  def platform(message: String): F[Unit]

}
