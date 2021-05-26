package org.greengen.store.feed

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, setOnInsert}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Page}
import org.greengen.db.mongo.Conversions
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.UpdateOptions


class MongoFeedStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends FeedStore[IO] {

  import Conversions._

  val FeedCollection = "feed"
  val feedCollection = db.getCollection(FeedCollection)

  override def mostRecentPost(userId: UserId): IO[Option[PostId]] = firstOptionIO {
    feedCollection
      .find(eql("user_id", userId.value.uuid))
      .sort(descending("timestamp"))
      .limit(1)
      .map(asPostId)
  }

  override def hasPosts(userId: UserId): IO[Boolean] = firstOptionIO {
    feedCollection
      .find(eql("user_id", userId.value.uuid))
      .limit(1)
  }.map(_.isDefined)

  override def getByUserId(userId: UserId, page: Page): IO[List[PostId]] = toListIO {
    feedCollection
      .find(eql("user_id", userId.value.uuid))
      .sort(descending("timestamp"))
      .paged(page)
      .map(asPostId)
  }

  override def addPost(userId: UserId, postId: PostId): IO[Unit] = unitIO {
    feedCollection
      .updateOne(and(
        eql("user_id", userId.value.uuid),
        eql("post_id", postId.value.uuid)),
        combine(
          setOnInsert("user_id", userId.value.uuid),
          setOnInsert("post_id", postId.value.uuid),
          setOnInsert("timestamp", clock.now().value),
        ),
        (new UpdateOptions).upsert(true)
      )
  }

}
