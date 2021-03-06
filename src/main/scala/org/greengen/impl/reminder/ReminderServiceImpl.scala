package org.greengen.impl.reminder

import java.util.concurrent.{Executors, TimeUnit}

import cats.effect.IO
import org.greengen.core._
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.{EventId, EventService}
import org.greengen.core.notification.NotificationService
import org.greengen.core.reminder.ReminderService


class ReminderServiceImpl(clock: Clock,
                          eventService: EventService[IO],
                          notificationService: NotificationService[IO])
  extends ReminderService[IO] {

  lazy val scheduler = Executors.newSingleThreadScheduledExecutor()

  override def reminder(eventId: EventId, schedule: Schedule): IO[Unit] = IO {
    val notificationTask: Runnable = () => for {
      eventOpt <- eventService.byId(eventId)
      event    <- IOUtils.from(eventOpt, s"Event $eventId doesn't exists or has been cancelled")
      users    <- eventService.participants(event.id, Page.All)
      _        <- notificationService.dispatch(???, users)
    } yield ()
    // Scheduling
    schedule match {
      case OneOff(start, _) =>
        val delay = start.value - clock.now().value
        val preNotif = UTCTimestamp.oneHourBefore(UTCTimestamp(delay))
        scheduler.schedule(notificationTask, preNotif.value, TimeUnit.MILLISECONDS)
        scheduler.schedule(notificationTask, delay, TimeUnit.MILLISECONDS)
      case Recurring(first, _, every, until) => // FIXME fix until
        val delay = first.value - clock.now().value
        val preNotif = UTCTimestamp.oneHourBefore(UTCTimestamp(delay))
        scheduler.scheduleAtFixedRate(notificationTask, preNotif.value, every.toMillis, TimeUnit.MILLISECONDS)
        scheduler.scheduleAtFixedRate(notificationTask, delay, every.toMillis, TimeUnit.MILLISECONDS)
    }
  }

  override def reminder(challengeId: ChallengeId, schedule: Schedule): IO[Unit] = ???

  override def cancel(eventId: EventId): IO[Unit] = ???

  override def cancel(challengeId: ChallengeId): IO[Unit] = ???

}
