package org.greengen.store.partnership

import cats.data.OptionT
import cats.effect.IO
import org.greengen.core.UTCTimestamp
import org.greengen.core.partnership.{Partner, PartnerId, PartnerPost}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryPartnershipStore extends PartnershipStore[IO] {

  private[this] lazy val byPartnerId = new TrieMap[PartnerId, Partner]()
  private[this] lazy val byUserId = new TrieMap[UserId, PartnerId]()
  private[this] lazy val byPartnerName = new TrieMap[String, PartnerId]()
  private[this] lazy val status = new TrieMap[PartnerId, (Boolean, UTCTimestamp)]()
  private[this] lazy val byPostId = new TrieMap[PostId, PartnerPost]()

  override def registerPartner(partner: Partner): IO[Unit] = for {
    _ <- IO(byPartnerId.put(partner.id, partner))
    _ <- IO(byUserId.put(partner.userId, partner.id))
    _ <- IO(byPartnerName.put(partner.name, partner.id))
  } yield ()

  override def registerPartnership(partnership: PartnerPost): IO[Unit] =
    IO(byPostId.put(partnership.postId, partnership))

  override def partnerById(partnerId: PartnerId): IO[Option[Partner]] =
    IO(byPartnerId.get(partnerId))

  override def partnerByUserId(userId: UserId): IO[Option[Partner]] = (for {
    partnerId <- OptionT.fromOption[IO](byUserId.get(userId))
    partner   <- OptionT.fromOption[IO](byPartnerId.get(partnerId))
  } yield partner).value

  override def enablePartner(partnerId: PartnerId, enabled: Boolean, timestamp: UTCTimestamp): IO[Unit] =
    IO(status.put(partnerId, (enabled, timestamp)))

  override def isPartnerEnabled(partnerId: PartnerId): IO[Boolean] =
    IO(status.get(partnerId).map(_._1).getOrElse(false))

  override def partnerFor(postId: PostId): IO[Option[PartnerPost]] =
    IO(byPostId.get(postId))


  // Empty implementations

  // No need to store anything in the in-memory implementation (data is not read anywhere)
  override def registerPostShownEvent(postId: PostId,
                                      userId: UserId,
                                      timestamp: UTCTimestamp): IO[Unit] =
    emptyImplementation()

  // No need to store anything in the in-memory implementation (data is not read anywhere)
  override def registerPartnerLinkFollowedEvent(postId: PostId,
                                                userId: UserId,
                                                timestamp: UTCTimestamp): IO[Unit] =
    emptyImplementation()

  private[this] def emptyImplementation() = IO.unit
}
