package org.greengen.core

sealed trait Schedule {
  def hasStarted(clock: Clock): Boolean
  def isOver(clock: Clock): Boolean
  def next(clock: Clock): Option[UTCTimestamp]
  def ticks(): LazyList[UTCTimestamp]
}

case class Duration(millis: Long) {
  def toMillis = millis
}

object Duration {

  val HalfAnHour = Duration(30 * 60 * 1000)
  val OneHour = Duration(60 * 60 * 1000)
  val OneDay = Duration(24 * 60 * 60 * 1000)
  val OneWeek = Duration(7 * 24 * 60 * 60 * 1000)

  def days(count: Int) = Duration(OneDay.millis * count)

  def weeks(count: Int) = Duration(OneWeek.millis * count)

}

// One-off event
case class OneOff(start: UTCTimestamp, end: UTCTimestamp) extends Schedule {

  override def hasStarted(clock: Clock): Boolean =
    clock.now().value >= start.value

  override def isOver(clock: Clock): Boolean =
    clock.now().value > end.value

  override def next(clock: Clock): Option[UTCTimestamp] =
    Option.when(clock.now().value <= end.value) { start }

  override def ticks(): LazyList[UTCTimestamp] =
    LazyList(start)

}

// Recurring event
case class Recurring(
  first: UTCTimestamp,
  duration: Duration,
  every: Duration,
  until: UTCTimestamp) extends Schedule {

  override def hasStarted(clock: Clock): Boolean =
    clock.now().value >= first.value

  override def isOver(clock: Clock): Boolean =
    clock.now().value > until.value

  override def next(clock: Clock): Option[UTCTimestamp] =
    ticks()
      .dropWhile(_.value < clock.now().value)
      .headOption
      .filter(_.value < until.value)

  override def ticks(): LazyList[UTCTimestamp] =
    LazyList.iterate(first) { _.plusMillis(every.toMillis) }
            .takeWhile(_.value <= until.value)
}


object Schedule {

  def daily(from: UTCTimestamp, until: UTCTimestamp): Option[Schedule] =
    Option.when (from.value < until.value) {
      Recurring(from, Duration.OneDay, Duration.OneDay, until)
    }

  def weekly(from: UTCTimestamp, until: UTCTimestamp): Option[Schedule] =
    Option.when (from.value < until.value) {
      Recurring(from, Duration.OneWeek, Duration.OneWeek, until)
    }

  def every(days: Int, from: UTCTimestamp, until: UTCTimestamp): Option[Schedule] =
    Option.when (from.value < until.value) {
      Recurring(from, Duration.days(days), Duration.days(days), until)
    }

}