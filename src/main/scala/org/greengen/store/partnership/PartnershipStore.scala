package org.greengen.store.partnership

import org.greengen.core.UTCTimestamp
import org.greengen.core.partnership.{Partner, PartnerId, PartnerPost}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


trait PartnershipStore[F[_]] {

  def registerPartner(partner: Partner): F[Unit]

  def registerPartnership(partnership: PartnerPost): F[Unit]

  def partnerById(partnerId: PartnerId): F[Option[Partner]]

  def partnerByUserId(userId: UserId): F[Option[Partner]]

  def enablePartner(partnerId: PartnerId, enabled: Boolean, timestamp: UTCTimestamp): F[Unit]

  def isPartnerEnabled(partnerId: PartnerId): F[Boolean]

  def partnerFor(postId: PostId): F[Option[PartnerPost]]

  // Registering events

  def registerPostShownEvent(postId: PostId, userId: UserId, timestamp: UTCTimestamp): F[Unit]

  def registerPartnerLinkFollowedEvent(postId: PostId, userId: UserId, timestamp: UTCTimestamp): F[Unit]

}
