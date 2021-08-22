package org.greengen.db.mongo

import java.math.BigInteger

import cats.effect.{ContextShift, IO}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.conversation.{ConversationId, MessageId}
import org.greengen.core.notification.NotificationId
import org.greengen.core.poll.PollId
import org.greengen.core.post.PostId
import org.greengen.core.tip.TipId
import org.greengen.core.user.UserId
import org.greengen.core.{Page, UUID}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Document, FindObservable, Observable}

import scala.jdk.CollectionConverters._


object Conversions {

  // Observable to IO

  def unitIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Unit] =
    IO.fromFuture(IO(obs.toFuture())).map(_ => ())

  def firstIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[T] =
    IO.fromFuture(IO(obs.head()))

  def firstOptionIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Option[T]] =
    IO.fromFuture(IO(obs.headOption()))

  def flattenFirstOptionIO[T](obs: => Observable[Option[T]])(implicit cs: ContextShift[IO]): IO[Option[T]] =
    IO.fromFuture(IO(obs.headOption())).map(_.flatten)

  def toIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Seq[T]] =
    IO.fromFuture(IO(obs.toFuture()))

  def toListIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[List[T]] =
    IO.fromFuture(IO(obs.toFuture())).map(_.toList)

  def toPagedListIO[T](page: Page)(obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[List[T]] =
    toListIO(obs).map(_.drop(math.min(0, page.n-1) * page.by).take(page.by))

  def toSetIO[T](obs: => Observable[T])(implicit cs: ContextShift[IO]): IO[Set[T]] =
    IO.fromFuture(IO(obs.toFuture())).map(_.toSet)


  // FIXME Unefficient pagination using skip
  implicit class paged[T](obs: FindObservable[T]) {
    def paged(page: Page): FindObservable[T] =
      obs.skip(math.max(0, page.n-1) * page.by).limit(page.by)
  }

  // Helpers

  def bytes2Hex(bytes: List[Byte]): String =
    bytes.map(_.toHexString).mkString("")

  def hexToBytes(hex: String): List[Byte] =
    new BigInteger(hex, 16).toByteArray.toList

  def eitherPair[A,B,L](a: Either[L,A], b: Either[L,B]): Either[L,(A,B)] = for {
    aR <- a
    bR <- b
  } yield (aR,bR)

  def asUserId(doc: Document) =
    UserId(UUID.unsafeFrom(doc.getString("user_id")))

  def asPostId(doc: Document) =
    PostId(UUID.unsafeFrom(doc.getString("post_id")))

  def asChallengeId(doc: Document) =
    ChallengeId(UUID.unsafeFrom(doc.getString("challenge_id")))

  def asPollId(doc: Document) =
    PollId(UUID.unsafeFrom(doc.getString("poll_id")))

  def asTipId(doc: Document) =
    TipId(UUID.unsafeFrom(doc.getString("tip_id")))

  def asNotificationId(doc: Document) =
    NotificationId(UUID.unsafeFrom(doc.getString("notification_id")))

  def asConversationId(doc: Document) =
    ConversationId(UUID.unsafeFrom(doc.getString("conversation_id")))

  def asMessageId(doc: Document) =
    MessageId(UUID.unsafeFrom(doc.getString("message_id")))

  def safeList[T](xs: List[T]) =
    Option(xs).getOrElse(List())

  def getList(doc: Document, field: String): List[BsonDocument] =
    doc.getList("reports", classOf[org.bson.Document]).asScala
      .map(_.toBsonDocument())
      .toList

}
