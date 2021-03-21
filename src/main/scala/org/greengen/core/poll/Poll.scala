package org.greengen.core.poll

import org.greengen.core.user.UserId
import org.greengen.core.{UTCTimestamp, UUID}

case class PollId(id: UUID)
case class PollOption(value: String) extends AnyVal
case class Poll(id: PollId,
                question: String,
                options: List[PollOption],
                afterAnswer: Option[String])

case class PollAnswer(userId: UserId, answer: PollOption, timestamp: UTCTimestamp)
case class PollAnswers(pollId: UUID, answers: List[UserId])

