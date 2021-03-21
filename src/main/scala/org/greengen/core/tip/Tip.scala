package org.greengen.core.tip

import org.greengen.core.user.UserId
import org.greengen.core.{MySelf, Source, UTCTimestamp, UUID}

case class TipId(value: UUID)

object TipId {
  def newId() = TipId(UUID.random())
}

case class Tip(
  id: TipId,
  author: UserId,
  content: String,
  created: UTCTimestamp,
  sources: List[Source] = List(MySelf))

