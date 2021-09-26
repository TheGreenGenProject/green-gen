package org.greengen.store.conversation

import org.greengen.core.Page
import org.greengen.core.conversation.{ConversationId, Message, MessageId}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait ConversationStore[F[_]] extends Store[F] {

  // Generic

  def getMessage(messageId: MessageId): F[Option[Message]]

  def getMessages(conversationId: ConversationId, page: Page): F[List[MessageId]]

  def addMessageToConversation(conversationId: ConversationId, message: Message): F[Unit]

  def flag(userId: UserId, messageId: MessageId): F[Unit]

  def unflag(userId: UserId, messageId: MessageId): F[Unit]

  def isFlagged(messageId: MessageId): F[Boolean]

  def hasUserFlagged(userId: UserId, messageId: MessageId): F[Boolean]

  def getFlagCount(messageId: MessageId): F[Long]

  // Post conversation

  def getConversation(postId: PostId): F[Option[ConversationId]]

  def addMessageToPost(postId: PostId, message: Message): F[Unit]

  def countMessages(postId: PostId): F[Long]

  // Private conversation

  def getPrivateConversation(author: UserId, dest: UserId): F[Option[ConversationId]]

  def addPrivateMessage(author: UserId, dest: UserId, message: Message): F[Unit]

}
