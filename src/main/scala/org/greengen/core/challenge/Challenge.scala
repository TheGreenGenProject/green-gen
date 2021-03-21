package org.greengen.core.challenge

import org.greengen.core.user.UserId
import org.greengen.core.{Schedule, UTCTimestamp, UUID}


case class ChallengeId(value: UUID)

object ChallengeId {
  def newId() = ChallengeId(UUID.random())
}



case class ChallengeContent(title: String, description: String)
// Description of the measure of success of the challenge
case class SuccessMeasure(
  maxFailure: Int = 0,
  maxSkip: Int    = 0,
  maxPartial: Int = 0)

case class Challenge(
  id: ChallengeId,
  author: UserId,
  created: UTCTimestamp,
  schedule: Schedule,
  content: ChallengeContent,
  measure: SuccessMeasure
)

// Global challenge status
sealed trait ChallengeStatus
case object NotYetStarted extends ChallengeStatus
case object OnGoing       extends ChallengeStatus
case object Closed        extends ChallengeStatus

// Status relative to a candidate
sealed trait ChallengeOutcomeStatus
case object NotYetTaken extends ChallengeOutcomeStatus
case object NotTaken extends ChallengeOutcomeStatus
case object Accepted    extends ChallengeOutcomeStatus
case object Rejected    extends ChallengeOutcomeStatus
case object Completed   extends ChallengeOutcomeStatus
case object OnTracks    extends ChallengeOutcomeStatus
case object Failed      extends ChallengeOutcomeStatus
case object Cancelled   extends ChallengeOutcomeStatus

sealed trait ChallengeStepReportStatus
case object Success extends ChallengeStepReportStatus
case object Partial extends ChallengeStepReportStatus
case object Skipped extends ChallengeStepReportStatus
case object Failure extends ChallengeStepReportStatus

case class ChallengeStepReportEntry(step: Int, status: ChallengeStepReportStatus)

// Per contestant report summary
case class ChallengeReportSummary(
  success: Int = 0,
  failure: Int = 0,
  skipped: Int = 0,
  partial: Int = 0)

object ChallengeReportSummary {

  // Summarize entries of a challenge
  def summary(entries: List[ChallengeStepReportEntry]): ChallengeReportSummary =
    entries.foldLeft(ChallengeReportSummary()) {
      case (summary, ChallengeStepReportEntry(_, Success)) => summary.copy(success = summary.success + 1)
      case (summary, ChallengeStepReportEntry(_, Partial)) => summary.copy(partial = summary.partial + 1)
      case (summary, ChallengeStepReportEntry(_, Skipped)) => summary.copy(skipped = summary.skipped + 1)
      case (summary, ChallengeStepReportEntry(_, Failure)) => summary.copy(failure = summary.failure + 1)
    }

  def aggregate(s1: ChallengeReportSummary, s2: ChallengeReportSummary): ChallengeReportSummary =
    ChallengeReportSummary(
      s1.success + s2.success,
      s1.failure + s2.failure,
      s1.skipped + s2.skipped,
      s1.partial + s2.partial)
  def aggregate(xs: List[ChallengeReportSummary]): ChallengeReportSummary =
    xs.foldLeft(ChallengeReportSummary())(aggregate)

  // Compute the status "on tracks" status of a challenge (assuming challenge is taken and over)
  // Basically, check if challenge success can still be achieved according to the success measure and the report summary
  def isOnTracks(challenge: Challenge, summary: ChallengeReportSummary): Boolean =
    if(summary.failure > challenge.measure.maxFailure) false
    else if(summary.partial > challenge.measure.maxPartial) false
    else if(summary.skipped > challenge.measure.maxSkip) false
    else true
}



// Global stats for a given challenge
// To allow standard charting
case class ChallengeStatistics(
  acceptedCount: Int = 0,
  rejectedCount: Int = 0,
  elapsedPeriodCount: Int = 0,
  totalPeriodCount: Int = 0,
  successCount: Int = 0,
  failureCount: Int = 0,
  partialSuccessCount: Int = 0,
  skippedCount: Int = 0)

object ChallengeStatistics {

  def statistics(acceptedCount: Int,
                 rejectedCount: Int,
                 elapsedPeriodCount: Int,
                 totalPeriodCount: Int,
                 summaries: List[ChallengeReportSummary]): ChallengeStatistics = {
    val aggregated = ChallengeReportSummary.aggregate(summaries)
    ChallengeStatistics(
      acceptedCount = summaries.size,
      elapsedPeriodCount = 0,
      totalPeriodCount = 0,
      successCount = aggregated.success,
      failureCount = aggregated.failure,
      partialSuccessCount = aggregated.partial,
      skippedCount = aggregated.skipped
    )
  }
}