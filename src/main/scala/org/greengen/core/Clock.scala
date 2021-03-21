package org.greengen.core

trait Clock {
  def now(): UTCTimestamp
}

object Clock {

  case object DefaultClock extends Clock {
    def now(): UTCTimestamp = UTCTimestamp(System.currentTimeMillis)
  }

  def apply(): Clock = DefaultClock
}
