package org.greengen.core.poll

import org.greengen.core.user.UserId
import org.greengen.core.{UTCTimestamp, UUID}

case class PollId(value: UUID)

object PollId {
  def newId() = PollId(UUID.random())
}


case class PollOption(value: String)
case class Poll(id: PollId,
                author: UserId,
                question: String,
                options: List[PollOption],
                timestamp: UTCTimestamp)

case class PollAnswer(userId: UserId, answer: PollOption, timestamp: UTCTimestamp)
case class PollAnswers(pollId: PollId, answers: List[PollAnswer])
case class PollStatsEntry(option: PollOption, count: Long)
case class PollStats(pollId: PollId, stats: List[PollStatsEntry])
