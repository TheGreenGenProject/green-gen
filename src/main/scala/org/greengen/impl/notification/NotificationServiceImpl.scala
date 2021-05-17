package org.greengen.impl.notification

import cats.effect.IO
import cats.implicits._
import org.greengen.core.notification.{Notification, NotificationId, NotificationService, PlatformMessageNotification}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Page, PagedResult}
import org.greengen.store.notification.NotificationStore


class NotificationServiceImpl(notifStore: NotificationStore[IO])
                             (clock: Clock, userService: UserService[IO])
  extends NotificationService[IO] {

  override def hasUnreadNotification(userId: UserId): IO[Boolean] = for {
    _ <- checkUser(userId)
    q <- notifStore.getQueueForUser(userId)
  } yield q.nonEmpty

  override def byId(id: NotificationId): IO[Option[Notification]] =
    notifStore.getNotification(id)

  override def dispatch(notif: Notification, users: List[UserId]): IO[Unit] = for {
    _ <- notifStore.register(notif.id, notif)
    _ <- users.map(userId => notifStore.addToUserQueue(userId, notif.id)).sequence
  } yield ()


  override def byUser(id: UserId, page: Page, unreadOnly: Boolean): IO[List[Notification]] = for {
    _      <- checkUser(id)
    notifs <- notifStore.getQueueForUser(id)
  } yield PagedResult.page(notifs, page)

  override def markAsRead(id: UserId, nid: NotificationId): IO[Unit] =
    notifStore.removeFromUserQueue(id, nid)

  override def platform(message: String): IO[Unit] = for {
    notif <- IO(Notification(NotificationId.newId, PlatformMessageNotification(message), clock.now()))
    users <- userService.activeUsers()
    _ <- dispatch(notif, users)
  } yield ()


  // Checkers

  private[this] def checkUser(user: UserId) = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
