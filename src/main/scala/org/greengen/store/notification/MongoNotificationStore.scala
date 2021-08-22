package org.greengen.store.notification

import cats.effect.{ContextShift, IO}
import org.greengen.core.notification.{Notification, NotificationId, NotificationWithReadStatus, Read, Unread}
import com.mongodb.client.model.Filters.{and, in, eq => eql}
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, setOnInsert, set}
import org.greengen.core.{Clock, Page, UTCTimestamp}
import org.greengen.core.user.UserId
import org.mongodb.scala.{Document, MongoDatabase}
import org.greengen.db.mongo.Conversions
import org.greengen.db.mongo.Schema


class MongoNotificationStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends NotificationStore[IO] {

  import Schema._
  import Conversions._

  val NotificationCollection = "notifications"
  val notificationCollection = db.getCollection(NotificationCollection)
  val ContentCollection = "notifications.content"
  val contentCollection = db.getCollection(ContentCollection)

  override def register(id: NotificationId, value: Notification): IO[Unit] = unitIO {
    contentCollection
      .updateOne(
        eql("notification_id", id.id.uuid),
        setOnInsert(notifToDoc(value)),
        (new UpdateOptions).upsert(true)
      )
  }

  override def hasUnreadNotifications(userId: UserId): IO[Boolean] = firstOptionIO {
    notificationCollection
      .find(and(
        eql("user_id", userId.value.uuid),
        eql("status", "unread")))
      .limit(1)
  }.map(_.isDefined)

  override def getNotification(id: NotificationId): IO[Option[Notification]] = firstIO {
    contentCollection
      .find(eql("notification_id", id.id.uuid))
      .limit(1)
      .map(docToNotif(_).toOption)
  }

  override def getQueueForUser(userId: UserId, page: Page, unreadOnly: Boolean): IO[List[NotificationWithReadStatus]] = for {
    statuses <- IO {
      if(unreadOnly) in("status", "unread")
      else in("status","read","unread") }
    idsAndStatuses <- toListIO {
      notificationCollection
        .find(and(
          eql("user_id", userId.value.uuid),
          statuses
        ))
        .sort(descending("timestamp"))
        .paged(page)
        .map(asNotificationIdWithReadStatus)
    }
    cache = idsAndStatuses.toMap
    notifs        <- getNotifications(idsAndStatuses.map(_._1))
    withStatus    <- IO(notifs.flatMap(notif => cache.get(notif.id).map(status => NotificationWithReadStatus(notif, status))))
  } yield withStatus

  override def addToUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] = unitIO {
    notificationCollection
      .updateOne(and(
        eql("user_id", userId.value.uuid),
        eql("notification_id", notificationId.id.uuid)),
      combine(
        setOnInsert("user_id", userId.value.uuid),
        setOnInsert("notification_id", notificationId.id.uuid),
        setOnInsert("timestamp", clock.now().value),
        setOnInsert("status", "unread"),
        setOnInsert("read_timestamp", null),
      ),
      (new UpdateOptions).upsert(true))
  }

  override def markAsRead(userId: UserId, notificationId: NotificationId): IO[Unit] = unitIO {
    notificationCollection
      .updateOne(and(
        eql("user_id", userId.value.uuid),
        eql("notification_id", notificationId.id.uuid)),
        combine(
          set("status", "read"),
          set("read_timestamp", clock.now().value),
        ))
  }

  override def removeFromUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] = unitIO {
    notificationCollection
      .deleteOne(and(
        eql("user_id", userId.value.uuid),
        eql("notification_id", notificationId.id.uuid)))
  }

  // Helpers

  private[this] def getNotifications(ids: List[NotificationId]): IO[List[Notification]] = toListIO {
    contentCollection
      .find(in("notification_id", ids.map(_.id.uuid):_*))
      .sort(descending("timestamp"))
      .map(docToNotif(_).toOption)
  }.map(_.flatten)

  private[this] def asNotificationIdWithReadStatus(doc: Document) =
    (asNotificationId(doc), asReadStatus(doc))

  private[this] def asReadStatus(doc: Document) =
    (doc.getString("status"), Option(doc.getLong("read_timestamp"))) match {
      case ("read", Some(tmstp)) => Read(UTCTimestamp(tmstp))
      case ("unread", _)         => Unread
      case _                     => Unread
    }
}
