package org.greengen.core.partnership

import org.greengen.core.{UTCTimestamp, UUID, Url}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

case class PartnerId(value: UUID)
object PartnerId {
  def newId() = PartnerId(UUID.random())
}

case class Partner(
  id: PartnerId,
  userId: UserId,
  name: String,
  description: String,
  url: Url,
  since: UTCTimestamp)

case class PartnerPost(
  partnerId: PartnerId,
  postId: PostId,
  from: UTCTimestamp,
  to: UTCTimestamp)
