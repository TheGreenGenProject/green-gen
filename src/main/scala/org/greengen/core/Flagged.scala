package org.greengen.core

import org.greengen.core.user.UserId


sealed trait Reason
case object Offensive extends Reason
case object Illegal extends Reason
case object PoliticallyBiased extends Reason
case object Guideline extends Reason
case class InaccurateSource(source: Source, counter: Source) extends Reason
case class Other(value: String) extends Reason


object Reason {
  def format(reason: Reason) = reason match {
    case Offensive => "offensive"
    case Illegal => "illegal"
    case PoliticallyBiased => "politically biased"
    case Guideline => "Doesn't follow guidelines"
    case InaccurateSource(source, counter) => "inaccurate sources"
    case Other(value) => s"other: $value"
  }

}

// Use to Flag a message, a user, some content etc.
// Which doesn't comply to guidelines
case class Flagged[T](
  id: T,
  reportedBy: UserId,
  timestamp: UTCTimestamp,
  reason: Reason)
