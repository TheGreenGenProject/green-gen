package org.greengen.store.conversation

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, or, eq => eql}
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, set, setOnInsert}
import org.greengen.core.conversation.{ConversationId, Message, MessageId}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, IOUtils, Page}
import org.greengen.db.mongo.Conversions
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.UpdateOptions



class MongoConversationStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO]) extends ConversationStore[IO] {

  import Conversions._
  import org.greengen.db.mongo.Schema._

  val MessagesCollection = "conversations.messages"
  val FlaggedMessagesCollection = "conversations.messages.flagged"
  val ConversationsCollection = "conversations"
  val PostConversationsCollection = "conversations.posts"
  val PrivateConversationsCollection = "conversations.privates"

  val messageCollection = db.getCollection(MessagesCollection)
  val flaggedMessageCollection = db.getCollection(FlaggedMessagesCollection)
  val conversationCollection = db.getCollection(ConversationsCollection)
  val postConversationCollection = db.getCollection(PostConversationsCollection)
  val privateConversationCollection = db.getCollection(PrivateConversationsCollection)


  override def getConversation(postId: PostId): IO[Option[ConversationId]] = firstOptionIO {
    postConversationCollection
      .find(eql("post_id", postId.value.uuid))
      .limit(1)
      .map(asConversationId)
  }

  override def getPrivateConversation(author: UserId, dest: UserId): IO[Option[ConversationId]] = firstOptionIO {
    privateConversationCollection
      .find(or(
        and(eql("user1", author.value.uuid),
            eql("user2", dest.value.uuid)),
        and(eql("user2", author.value.uuid),
            eql("user1", dest.value.uuid))))
      .limit(1)
      .map(asConversationId)
  }

  override def countMessages(postId: PostId): IO[Long] = (for {
    conversationId <- OptionT(getConversation(postId))
    res            <- OptionT.liftF(countConversationMessages(conversationId))
  } yield res).getOrElse(0L)

  override def getMessage(messageId: MessageId): IO[Option[Message]] = flattenFirstOptionIO {
    messageCollection
      .find(eql("message_id", messageId.value.uuid))
      .limit(1)
      .map(docToMessage(_).toOption)
  }

  override def getMessages(conversationId: ConversationId, page: Page): IO[List[MessageId]] = toListIO {
    conversationCollection
      .find(eql("conversation_id", conversationId.value.uuid))
      .sort(descending("timestamp"))
      .paged(page)
      .map(asMessageId(_))
  }

  override def addMessageToConversation(conversationId: ConversationId, message: Message): IO[Unit] = unitIO {
    conversationCollection
      .insertOne(Document(
        "conversation_id" -> conversationId.value.uuid,
         "message_id" -> message.id.value.uuid,
         "timestamp" -> message.timestamp.value))
  }

  override def addPrivateMessage(author: UserId, dest: UserId, message: Message): IO[Unit] = for {
    conversationId <- getOrCreatePrivateConversation(author, dest)
    _              <- addMessageToStore(message)
    _              <- addMessageToConversation(conversationId, message)
  } yield ()

  override def addMessageToPost(postId: PostId, message: Message): IO[Unit] = for {
    conversationId <- getOrCreateConversationForPost(postId)
    _              <- addMessageToStore(message)
    _              <- addMessageToConversation(conversationId, message)
  } yield ()

  override def flag(userId: UserId, messageId: MessageId): IO[Unit] =
    flagMessage(userId, messageId, true)

  override def unflag(userId: UserId, messageId: MessageId): IO[Unit] =
    flagMessage(userId, messageId, false)

  override def isFlagged(messageId: MessageId): IO[Boolean] = firstOptionIO {
    flaggedMessageCollection
      .find(eql("message_id", messageId.value.uuid))
      .limit(1)
  }.map(_.isDefined)

  override def hasUserFlagged(userId: UserId, messageId: MessageId): IO[Boolean] = firstOptionIO {
    flaggedMessageCollection
      .find(and(
        eql("message_id", messageId.value.uuid),
        eql("user_id", userId.value.uuid)))
      .limit(1)
  }.map(_.isDefined)

  override def getFlagCount(messageId: MessageId): IO[Long] = firstIO {
    flaggedMessageCollection
      .countDocuments(eql("message_id", messageId.value.uuid))
  }

  // Helpers

  private[this] def getOrCreateConversationForPost(postId: PostId): IO[ConversationId] =
    for { _ <- unitIO {
          postConversationCollection
            .updateOne(
              eql("post_id", postId.value.uuid),
              combine(
                setOnInsert("post_id", postId.value.uuid),
                setOnInsert("conversation_id", ConversationId.newId.value.uuid),
              ),
              (new UpdateOptions).upsert(true)
            )
          }
         maybeConversationId <- getConversation(postId)
         conversationId      <- IOUtils.from(maybeConversationId, s"Couldn't find a conversationId for post ${postId}")
    } yield conversationId

  private[this] def getOrCreatePrivateConversation(author: UserId, dest: UserId): IO[ConversationId] =
    for { _ <- unitIO {
      privateConversationCollection
        .updateOne(or(
          and(eql("user1", author.value.uuid),
            eql("user2", dest.value.uuid)),
          and(eql("user2", author.value.uuid),
            eql("user1", dest.value.uuid))),
          combine(
            setOnInsert("user1", deterministicPair(author, dest)._1.value.uuid),
            setOnInsert("user2", deterministicPair(author, dest)._2.value.uuid),
            setOnInsert("conversation_id", ConversationId.newId.value.uuid),
          ),
          (new UpdateOptions).upsert(true)
        )
      }
      users = deterministicPair(author, dest)
      maybeConversationId <- getPrivateConversation(users._1, users._2)
      conversationId      <- IOUtils.from(maybeConversationId, s"Couldn't find a conversationId between users ${users._1} and ${users._2}")
    } yield conversationId

  private[this] def addMessageToStore(message: Message): IO[Unit] = unitIO {
    messageCollection
      .insertOne(messageToDoc(message))
  }

  private[this] def countConversationMessages(conversationId: ConversationId): IO[Long] = firstIO {
    conversationCollection
      .countDocuments(eql("conversation_id", conversationId.value.uuid))
  }

  private[this] def flagMessage(userId: UserId, messageId: MessageId, flag: Boolean): IO[Unit] = unitIO {
    flaggedMessageCollection
      .updateOne(and(
        eql("message_id", messageId.value.uuid),
        eql("user_id", messageId.value.uuid)),
        combine(
          setOnInsert("message_id", messageId.value.uuid),
          setOnInsert("user_id", userId.value.uuid),
          set("flagged", flag),
          set("timestamp", clock.now().value)
        ),
        (new UpdateOptions).upsert(true)
      )
  }

  private[this] def deterministicPair(author: UserId, dest: UserId): (UserId, UserId) =
    if(author.value.uuid <= dest.value.uuid) (author, dest)
    else (dest, author)
}
