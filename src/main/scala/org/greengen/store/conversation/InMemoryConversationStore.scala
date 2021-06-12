package org.greengen.store.conversation

import cats.effect.IO
import org.greengen.core.conversation.{ConversationId, Message, MessageId}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.core.{IOUtils, Page, PagedResult}

import scala.collection.concurrent.TrieMap


class InMemoryConversationStore extends ConversationStore[IO] {

  private[this] val byPosts = new TrieMap[PostId, ConversationId]
  private[this] val messages = new TrieMap[MessageId, Message]
  private[this] val byConversations = new TrieMap[ConversationId, List[MessageId]]
  private[this] val byReporter = new TrieMap[UserId, Set[MessageId]]
  private[this] val reports = new TrieMap[MessageId, Set[UserId]]

  override def getConversation(postId: PostId): IO[Option[ConversationId]] =
    IO(byPosts.get(postId))

  override def countMessages(postId: PostId): IO[Int] = for {
    conversationId <- getConversation(postId)
    count          <- IO(conversationId.flatMap(byConversations.get(_)).map(_.size).getOrElse(0))
  } yield count

  override def getMessage(messageId: MessageId): IO[Option[Message]] =
    IO(messages.get(messageId))

  override def getMessages(conversationId: ConversationId, page: Page): IO[List[MessageId]] = for {
    allIds <- IO(byConversations.getOrElse(conversationId, List()))
    ids    <- IO(PagedResult.page(allIds, page))
  } yield ids

  override def addMessageToPost(postId: PostId, message: Message): IO[Unit] = for {
    conversationId <- getConversation(postId).flatMap {
        case Some(id) => IO(id)
        case None     => newConversation(postId)
      }
    _ <- addMessageToConversation(conversationId, message)
  } yield ()

  override def addMessageToConversation(conversationId: ConversationId, message: Message): IO[Unit] = for {
    _ <- checkConversation(conversationId)
    _ <- indexByConversationId(conversationId, message)
    _ <- indexMessage(message)
  } yield ()

  override def flag(userId: UserId, messageId: MessageId): IO[Unit] = for {
    _ <- indexByReporter(userId, messageId)
    _ <- indexByReport(userId, messageId)
  } yield ()

  override def unflag(userId: UserId, messageId: MessageId): IO[Unit] = for {
    _ <- unflagByReporter(userId, messageId)
    _ <- unflagByReport(userId, messageId)
  } yield ()

  override def isFlagged(messageId: MessageId): IO[Boolean] =
    IO(reports.getOrElse(messageId, Set()).nonEmpty)

  override def getFlagCount(messageId: MessageId): IO[Int] =
    IO(reports.getOrElse(messageId, Set()).size)

  override def hasUserFlagged(userId: UserId, messageId: MessageId): IO[Boolean] = {
    IO(byReporter.getOrElse(userId, Set()).contains(messageId))
  }

  // Helpers

  private[this] def newConversation(postId: PostId): IO[ConversationId] = for {
    newid <- IO(ConversationId.newId)
    _     <- IO(byPosts.putIfAbsent(postId, newid))
    id    <- IO.fromOption(byPosts.get(postId))(new RuntimeException(s"No conversation id for post ${postId}"))
    _     <- IO(byConversations.putIfAbsent(id, List()))
  } yield id

  // Checkers

  private[this] def checkConversation(conversationId: ConversationId): IO[Unit] = for {
    exists <- IO(byConversations.contains(conversationId))
    _      <- IOUtils.check(exists, s"Conversation ${conversationId} cannot be found")
  } yield ()

  // Indexer

  private[this] def indexMessage(message: Message): IO[Unit] =
    IO(messages.put(message.id, message))

  private[this] def indexByConversationId(conversationId: ConversationId, message: Message): IO[Unit] = IO {
    byConversations.updateWith(conversationId) {
      case Some(ids) => Some(message.id :: ids)
      case None => Some(List(message.id))
    }
  }

  private[this] def indexByReporter(userId: UserId, messageId: MessageId): IO[Unit] = IO {
    byReporter.updateWith(userId) {
      case Some(ids) => Some(ids + messageId)
      case None => Some(Set(messageId))
    }
  }

  private[this] def indexByReport(userId: UserId, messageId: MessageId): IO[Unit] = IO {
    reports.updateWith(messageId) {
      case Some(ids) => Some(ids + userId)
      case None => Some(Set(userId))
    }
  }

  private[this] def unflagByReporter(userId: UserId, messageId: MessageId): IO[Unit] = IO {
    byReporter.updateWith(userId) {
      case Some(ids) => Some(ids - messageId)
      case None => None
    }
  }

  private[this] def unflagByReport(userId: UserId, messageId: MessageId): IO[Unit] = IO {
    reports.updateWith(messageId) {
      case Some(ids) => Some(ids - userId)
      case None => None
    }
  }

}
