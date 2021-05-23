package org.greengen.store.like

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.{combine, set, setOnInsert}
import org.greengen.core.Clock
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.db.mongo.Conversions
import org.mongodb.scala.MongoDatabase


class MongoLikeStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends LikeStore[IO] {

  import Conversions._

  val LikeCollection = "posts.likes"
  val likeCollection = db.getCollection(LikeCollection)

  override def getByPostId(id: PostId): IO[Set[UserId]] = toSetIO {
    likeCollection
      .find(eql("post_id", id.value.uuid))
      .map(asUserId(_))
  }

  override def countLikes(id: PostId): IO[Long] = firstIO {
    likeCollection
      .countDocuments(eql("post_id", id.value.uuid))
  }

  override def addLike(userId: UserId, postId: PostId): IO[Unit] = unitIO {
    likeCollection
      .updateOne(and(
        eql("post_id", postId.value.uuid),
        eql("user_id", userId.value.uuid)),
        combine(
          setOnInsert("post_id", postId.value.uuid),
          setOnInsert("user_id" , userId.value.uuid),
          set("timestamp", clock.now().value)),
        (new UpdateOptions).upsert(true)
      )
  }

  override def removeLike(userId: UserId, postId: PostId): IO[Unit] = unitIO {
    likeCollection
      .deleteOne(and(
        eql("post_id", postId.value.uuid),
        eql("user_id", userId.value.uuid)))
  }
}
