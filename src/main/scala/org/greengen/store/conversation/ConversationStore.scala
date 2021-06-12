package org.greengen.store.conversation

import org.greengen.core.Page
import org.greengen.core.conversation.{ConversationId, Message, MessageId}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.greengen.store.Store


trait ConversationStore[F[_]] extends Store[F] {

  def getConversation(postId: PostId): F[Option[ConversationId]]

  def countMessages(postId: PostId): F[Int]

  def getMessage(messageId: MessageId): F[Option[Message]]

  def getMessages(conversationId: ConversationId, page: Page): F[List[MessageId]]

  def addMessageToConversation(conversationId: ConversationId, message: Message): F[Unit]

  def addMessageToPost(postId: PostId, message: Message): F[Unit]

  def flag(userId: UserId, messageId: MessageId): F[Unit]

  def unflag(userId: UserId, messageId: MessageId): F[Unit]

  def isFlagged(messageId: MessageId): F[Boolean]

  def hasUserFlagged(userId: UserId, messageId: MessageId): F[Boolean]

  def getFlagCount(messageId: MessageId): F[Int]

}
