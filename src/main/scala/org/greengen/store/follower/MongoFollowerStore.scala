package org.greengen.store.follower

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, eq => eql}
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.{combine, setOnInsert}
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, UUID}
import org.greengen.db.mongo.Conversions
import org.mongodb.scala.MongoDatabase


class MongoFollowerStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends FollowerStore[IO] {

  import Conversions._

  val FollowerCollection = "followers"
  val followerCollection = db.getCollection(FollowerCollection)

  override def getFollowersByUser(userId: UserId): IO[Set[UserId]] = toSetIO {
    followerCollection
      .find(eql("user_id", userId.value.uuid))
      .map(_.getString("follower_id"))
      .map(UUID.unsafeFrom(_))
      .map(UserId(_))
  }

  override def getFollowingByUser(userId: UserId): IO[Set[UserId]] = toSetIO {
    followerCollection
      .find(eql("follower_id", userId.value.uuid))
      .map(_.getString("user_id"))
      .map(UUID.unsafeFrom(_))
      .map(UserId(_))
  }

  override def startFollowing(src: UserId, dst: UserId): IO[Unit] = unitIO {
    followerCollection
      .updateOne(
        and(eql("user_id", dst.value.uuid), eql("follower_id", src.value.uuid)),
        combine(
          setOnInsert("user_id", dst.value.uuid),
          setOnInsert("follower_id", src.value.uuid),
          setOnInsert("timestamp", clock.now().value),
        ),
        (new UpdateOptions).upsert(true)
      )
  }

  override def stopFollowing(src: UserId, dst: UserId): IO[Unit] = unitIO {
    followerCollection
      .deleteOne(and(
        eql("user_id", dst.value.uuid),
        eql("follower_id", src.value.uuid)))
  }

}
