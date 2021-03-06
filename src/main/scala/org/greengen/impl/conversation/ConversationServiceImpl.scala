package org.greengen.impl.conversation

import cats.effect.IO
import org.greengen.core.conversation.{ConversationId, ConversationService, Message, MessageId}
import org.greengen.core.notification.NotificationService
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Page}
import org.greengen.store.conversation.ConversationStore

class ConversationServiceImpl(conversationStore: ConversationStore[IO])
                             (clock: Clock,
                              userService: UserService[IO],
                              notificationService: NotificationService[IO]) extends ConversationService[IO] {


  override def getConversationMessages(conversationId: ConversationId, page: Page): IO[List[MessageId]] =
    conversationStore.getMessages(conversationId, page)

  override def flag(userId: UserId, messageId: MessageId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- conversationStore.flag(userId, messageId)
  } yield ()

  override def unflag(userId: UserId, messageId: MessageId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- conversationStore.unflag(userId, messageId)
  } yield ()

  override def isFlagged(messageId: MessageId): IO[Boolean] =
    conversationStore.isFlagged(messageId)

  override def hasUserFlagged(userId: UserId, messageId: MessageId): IO[Boolean] =
    conversationStore.hasUserFlagged(userId, messageId)

  // Post conversation

  override def getConversation(postId: PostId): IO[ConversationId] =
    conversationStore.getConversation(postId)
      .map(_.getOrElse(ConversationId.newId))

  override def countMessages(postId: PostId): IO[Long] =
    conversationStore.countMessages(postId)

  override def getMessage(messageId: MessageId): IO[Option[Message]] =
    conversationStore.getMessage(messageId)

  override def addMessage(postId: PostId, message: Message): IO[Unit] =
    conversationStore.addMessageToPost(postId, message)

  // Private conversation

  override def getConversation(author: UserId, dest: UserId): IO[ConversationId] =
    conversationStore.getPrivateConversation(author, dest)
      .map(_.getOrElse(ConversationId.newId))

  override def addPrivateMessage(author: UserId, dest: UserId, message: Message): IO[Unit] =
    conversationStore.addPrivateMessage(author, dest, message)


  // Check users

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _       <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
