package org.greengen.store

import org.greengen.core.UUID
import org.greengen.core.conversation.Conversation


trait ConversationStore[F[_]] {

  def newConversation(id: UUID):F[Conversation]

}
