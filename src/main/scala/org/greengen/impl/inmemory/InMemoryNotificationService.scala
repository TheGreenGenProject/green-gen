package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.{Clock, IOUtils, Page, PagedResult}
import org.greengen.core.notification.{Notification, NotificationId, NotificationService, PlatformMessageNotification}
import org.greengen.core.user.{UserId, UserService}

import scala.collection.concurrent.TrieMap


class InMemoryNotificationService(clock: Clock, userService: UserService[IO])
  extends NotificationService[IO] {

  val notifications = new TrieMap[NotificationId, Notification]
  val dispatchQueues = new TrieMap[UserId, List[Notification]]


  override def hasUnreadNotification(userId: UserId): IO[Boolean] = for {
    _ <- checkUser(userId)
    q <- IO(dispatchQueues.getOrElse(userId, List[Notification]()))
  } yield q.nonEmpty

  override def byId(id: NotificationId): IO[Option[Notification]] = IO {
    notifications.get(id)
  }

  override def dispatch(notif: Notification, users: List[UserId]): IO[Unit] = IO {
    notifications.put(notif.id, notif)
    users.foreach { id =>
      dispatchQueues.updateWith(id) {
        case Some(q) => Some(notif :: q)
        case None => Some(List(notif))
      }
    }
  }

  override def byUser(id: UserId, page: Page, unreadOnly: Boolean): IO[List[Notification]] = for {
    _      <- checkUser(id)
    notifs <- IO(dispatchQueues.getOrElse(id, List()))
  } yield PagedResult.page(notifs, page)

  override def markAsRead(id: UserId, nid: NotificationId): IO[Unit] = IO {
    dispatchQueues.updateWith(id)(q => q.map(_.filterNot(_.id==nid)))
  }

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
