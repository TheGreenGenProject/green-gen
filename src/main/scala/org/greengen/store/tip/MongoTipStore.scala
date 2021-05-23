package org.greengen.store.tip

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{eq => eql}
import org.greengen.core.tip.{Tip, TipId}
import org.greengen.core.user.UserId
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase


class MongoTipStore(db: MongoDatabase)(implicit cs: ContextShift[IO]) extends TipStore[IO] {

  import Conversions._
  import Schema._

  val TipCollection = "posts.tips"
  val tipCollection = db.getCollection(TipCollection)

  override def getByTipId(id: TipId): IO[Option[Tip]] = firstIO {
    tipCollection
      .find(eql("tip_id", id.value.uuid))
      .limit(1)
      .map(docToTip(_).toOption)
  }

  override def getByAuthor(id: UserId): IO[Set[TipId]] = toSetIO {
    tipCollection
      .find(eql("author", id.value.uuid))
      .map(asTipId(_))
  }

  override def store(tip: Tip): IO[Unit] = unitIO {
    tipCollection
      .insertOne(tipToDoc(tip))
  }

}
