package org.greengen.core.recommendation

import java.util.UUID

import org.greengen.core.UTCTimestamp


case class RecommendationId(value: UUID)
case class Recommendation[T](
  id: RecommendationId,
  recommended: T,
  reason: Option[String],
  timestamp: UTCTimestamp
)
