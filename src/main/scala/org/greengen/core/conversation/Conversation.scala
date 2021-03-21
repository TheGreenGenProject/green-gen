package org.greengen.core.conversation

import org.greengen.core.user.UserId
import org.greengen.core.{UTCTimestamp, UUID}

case class MessageId(value: UUID)
case class Message(
  id: MessageId,
  user: UserId,
  content: String,
  timestamp: UTCTimestamp)

case class ConversationId(value: UUID)
case class Conversation(
  id: UUID,
  messages: List[Message])
