package org.greengen.core.conversation

import org.greengen.core.Page
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


trait ConversationService[F[_]] {

  // Generic

  def getMessage(messageId: MessageId): F[Option[Message]]

  def getConversationMessages(conversationId: ConversationId, page: Page): F[List[MessageId]]

  def flag(userId: UserId, message: MessageId): F[Unit]

  def unflag(userId: UserId, message: MessageId): F[Unit]

  def isFlagged(messageId: MessageId): F[Boolean]

  def hasUserFlagged(userId: UserId, messageId: MessageId): F[Boolean]

  // Post specific conversation

  def getConversation(postId: PostId): F[ConversationId]

  def countMessages(postId: PostId): F[Long]

  def addMessage(postId: PostId, message: Message): F[Unit]

  // Private conversations

  def getConversation(author: UserId, dest: UserId): F[ConversationId]

  def addPrivateMessage(author: UserId, dest: UserId, message: Message): F[Unit]

}