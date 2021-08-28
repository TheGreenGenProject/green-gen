package org.greengen.core.partnership

import org.greengen.core.{UTCTimestamp, Url}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


trait PartnershipService[F[_]] {

  // Partner

  def registerPartner(userId: UserId, name: String, description: String, url: Url): F[(UserId,PartnerId)]

  def partnerById(partnerId: PartnerId): F[Option[Partner]]

  def partnerByUserId(userId: UserId): F[Option[Partner]]

  def enablePartner(partnerId: PartnerId): F[Unit]

  def disablePartner(partnerId: PartnerId): F[Unit]

  def isPartnerEnabled(partnerId: PartnerId): F[Boolean]

  // Post partnership

  def partnerFor(postId: PostId): F[Option[PartnerId]]

  // Creates a partnership, ie post becomes visible as a partner post
  def partnership(partnerId: PartnerId, postId: PostId, from: UTCTimestamp, to: UTCTimestamp): F[PartnerPost]

  def hasPartner(postId: PostId): F[Boolean]

  def isPartnershipActive(postId: PostId): F[Boolean]

  // Callbacks

  // Call when the post is returned in the feed of a user
  def postShown(postId: PostId, userId: UserId): F[Unit]

  // Callback when the partner Url is visited
  def visited(postId: PostId, userId: UserId): F[Unit]

}
