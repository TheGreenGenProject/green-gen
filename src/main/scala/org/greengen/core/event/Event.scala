package org.greengen.core.event

import org.greengen.core.user.UserId
import org.greengen.core.{Location, Schedule, UUID}

case class EventId(value: UUID)

object EventId {
  def newId() = EventId(UUID.random())
}

case class Event(id: EventId,
                 owner: UserId,
                 participants: List[UserId],
                 maxParticipants: Int,
                 description: String,
                 location: Location,
                 schedule: Schedule,
                 enabled: Boolean
)






