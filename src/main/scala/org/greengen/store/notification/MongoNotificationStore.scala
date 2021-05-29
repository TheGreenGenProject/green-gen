package org.greengen.store.notification

import cats.effect.{ContextShift, IO}
import org.greengen.core.notification.{Notification, NotificationId}
import com.mongodb.client.model.Filters.{and, eq => eql, in}
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, setOnInsert}
import org.greengen.core.{Clock, Page}
import org.greengen.core.user.UserId
import org.mongodb.scala.MongoDatabase
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
      .find(eql("user_id", userId.value.uuid))
      .limit(1)
  }.map(_.isDefined)

  override def getNotification(id: NotificationId): IO[Option[Notification]] = firstIO {
    contentCollection
      .find(eql("notification_id", id.id.uuid))
      .limit(1)
      .map(docToNotif(_).toOption)
  }

  override def getQueueForUser(userId: UserId, page: Page): IO[List[Notification]] = for {
    ids <- toListIO {
      notificationCollection
        .find(eql("user_id", userId.value.uuid))
        .sort(descending("timestamp"))
        .paged(page)
        .map(asNotificationId)
    }
    notifs <- getNotifications(ids)
  } yield notifs

  override def addToUserQueue(userId: UserId, notificationId: NotificationId): IO[Unit] = unitIO {
    notificationCollection
      .updateOne(and(
        eql("user_id", userId.value.uuid),
        eql("notification_id", notificationId.id.uuid)),
      combine(
        setOnInsert("user_id", userId.value.uuid),
        setOnInsert("notification_id", notificationId.id.uuid),
        setOnInsert("timestamp", clock.now().value)
      ),
      (new UpdateOptions).upsert(true))
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

}
