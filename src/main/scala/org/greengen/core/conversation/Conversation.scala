package org.greengen.core.conversation

import org.greengen.core.user.UserId
import org.greengen.core.{UTCTimestamp, UUID}

object MessageId {
  def newId = MessageId(UUID.random())
}

case class MessageId(value: UUID)
case class Message(
  id: MessageId,
  user: UserId,
  content: String,
  timestamp: UTCTimestamp)


object ConversationId {
  def newId = ConversationId(UUID.random())
}

case class ConversationId(value: UUID)
case class Conversation(
  id: ConversationId,
  messages: List[Message])
