package org.greengen.store.wall

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.Updates.{combine, setOnInsert}
import com.mongodb.client.model.Sorts.descending
import org.greengen.core.{Clock, Page}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.db.mongo.Conversions
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.UpdateOptions


class MongoWallStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends WallStore[IO] {

  import Conversions._

  val WallCollection = "wall"
  val wallCollection = db.getCollection(WallCollection)


  override def getByUserId(userId: UserId, page: Page): IO[List[PostId]] = toListIO {
    wallCollection
      .find(eql("user_id", userId.value.uuid))
      .sort(descending("timestamp"))
      .paged(page)
      .map(asPostId(_))
  }

  override def addPost(userId: UserId, postId: PostId): IO[Unit] = unitIO {
    wallCollection
      .updateOne(and(
          eql("user_id", userId.value.uuid),
          eql("post_id", postId.value.uuid)),
        combine(
          setOnInsert("post_id", postId.value.uuid),
          setOnInsert("user_id", userId.value.uuid),
          setOnInsert("timestamp", clock.now().value),
        ),
        (new UpdateOptions).upsert(true)
      )
  }

}
