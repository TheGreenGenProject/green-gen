package org.greengen.store.conversation

import cats.effect.IO
import org.greengen.core.{IOUtils, Page}
import org.greengen.core.conversation.{ConversationId, Message, MessageId}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap

object CachedConversationStore {
  def withCache(store: ConversationStore[IO]): ConversationStore[IO] =
    new CachedConversationStore(store)
}

// Just caching count for post conversation
private[conversation] class CachedConversationStore(store: ConversationStore[IO]) extends ConversationStore[IO] {

  private[this] val countCache = new TrieMap[PostId, Long]

  override def getMessage(messageId: MessageId): IO[Option[Message]] =
    store.getMessage(messageId)

  override def getMessages(conversationId: ConversationId, page: Page): IO[List[MessageId]] =
    store.getMessages(conversationId, page)

  override def addMessageToConversation(conversationId: ConversationId, message: Message): IO[Unit] =
    store.addMessageToConversation(conversationId, message)

  override def flag(userId: UserId, messageId: MessageId): IO[Unit] =
    store.flag(userId, messageId)

  override def unflag(userId: UserId, messageId: MessageId): IO[Unit] =
    store.unflag(userId, messageId)

  override def isFlagged(messageId: MessageId): IO[Boolean] =
    store.isFlagged(messageId)

  override def hasUserFlagged(userId: UserId, messageId: MessageId): IO[Boolean] =
    store.hasUserFlagged(userId, messageId)

  override def getFlagCount(messageId: MessageId): IO[Long] =
    store.getFlagCount(messageId)

  override def getConversation(postId: PostId): IO[Option[ConversationId]] =
    store.getConversation(postId)

  override def addMessageToPost(postId: PostId, message: Message): IO[Unit] = for {
    _ <- store.addMessageToPost(postId, message)
    _ <- IO {
      countCache.updateWith(postId) {
        case Some(prev) => Some(prev+1)
        case None => Some(1)
      }
    }
  } yield ()

  override def countMessages(postId: PostId): IO[Long] = for {
    cached <- IO(countCache.get(postId).map(_.toLong))
    result <- {
      if(cached.nonEmpty) IO(cached.get)
      else store.countMessages(postId)
    }
    _ <- IO.whenA(cached.isEmpty) {
      IO(countCache.put(postId, result))
    }
  } yield result

  override def getPrivateConversation(author: UserId, dest: UserId): IO[Option[ConversationId]] =
    store.getPrivateConversation(author, dest)

  override def addPrivateMessage(author: UserId, dest: UserId, message: Message): IO[Unit] =
    store.addPrivateMessage(author, dest, message)

}
