package org.greengen.store.pin

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.{combine, setOnInsert}
import org.greengen.core.Page
import org.greengen.core.pin.PinnedPost
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase


class MongoPinStore(db: MongoDatabase)(implicit cs: ContextShift[IO]) extends PinStore[IO] {

  import Conversions._
  import Schema._

  val PinCollection = "posts.pinned"
  val pinCollection = db.getCollection(PinCollection)

  override def addPin(userId: UserId, pinnedPost: PinnedPost): IO[Unit] = unitIO {
    pinCollection
      .updateOne(and(
        eql("user_id", userId.value.uuid),
        eql("post_id", pinnedPost.postId.value.uuid)
       ),
        combine(
          setOnInsert("user_id", userId.value.uuid),
          setOnInsert("post_id", pinnedPost.postId.value.uuid),
          setOnInsert("timestamp", pinnedPost.timestamp.value)
        ),
        (new UpdateOptions).upsert(true)
      )
  }

  override def removePin(userId: UserId, postId: PostId): IO[Unit] = unitIO {
    pinCollection
      .deleteOne(and(
        eql("user_id", userId.value.uuid),
        eql("post_id", postId.value.uuid)
      ))
  }

  override def isPinned(userId: UserId, postId: PostId): IO[Boolean] = firstOptionIO {
    pinCollection
      .find(and(
        eql("user_id", userId.value.uuid),
        eql("post_id", postId.value.uuid)))
      .limit(1)
  }.map(_.isDefined)

  override def getByUser(userId: UserId, page: Page): IO[List[PinnedPost]] = toListIO {
    pinCollection
      .find(eql("user_id", userId.value.uuid))
      .sort(descending("timestamp"))
      .paged(page)
      .map(docToPinnedPost(_).toOption)
  }.map(_.flatten)
}
