package org.greengen.core

case class UTCTimestamp(value: Long) {
  def plusMillis(amount: Long) = UTCTimestamp(value + amount)
}

object UTCTimestamp {

  def now() = UTCTimestamp(System.currentTimeMillis)

  // FIXME
  def midnight(ts: UTCTimestamp) = ts

  def dayBefore(ts: UTCTimestamp) =
    UTCTimestamp(ts.value - 24 * 3600 * 1000L)

  def oneHourBefore(ts: UTCTimestamp) =
    UTCTimestamp(ts.value - 3600 * 1000L)

  def dayAfter(ts: UTCTimestamp) =
    UTCTimestamp(ts.value + 24 * 3600 * 1000L)

  def plusDays(ts: UTCTimestamp, n: Int) =
    UTCTimestamp(ts.value + n * 24 * 3600 * 1000L)
}
