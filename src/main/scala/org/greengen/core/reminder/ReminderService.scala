package org.greengen.core.reminder

import org.greengen.core.Schedule
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId

// Scheduling / Cancelling reminders
trait ReminderService[F[_]] {

  def reminder(eventId: EventId, schedule: Schedule): F[Unit]

  def reminder(challengeId: ChallengeId, schedule: Schedule): F[Unit]

  def cancel(eventId: EventId): F[Unit]

  def cancel(challengeId: ChallengeId): F[Unit]

}
