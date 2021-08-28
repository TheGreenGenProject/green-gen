package org.greengen.impl.partnership

import cats.data.OptionT
import cats.effect.IO
import org.greengen.core.partnership.{Partner, PartnerId, PartnerPost, PartnershipService}
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, UTCTimestamp, Url}
import org.greengen.store.partnership.PartnershipStore

class PartnershipServiceImpl(partnershipStore: PartnershipStore[IO])
                            (clock: Clock,
                             userService: UserService[IO])
  extends PartnershipService[IO] {

  override def registerPartner(userId: UserId, name: String, description: String, url: Url): IO[(UserId, PartnerId)] = for {
    _ <- checkUser(userId)
    _ <- checkNewPartner(name, description, url)
    partnerId = PartnerId.newId()
    partner = Partner(partnerId, userId, name, description, url, clock.now())
    _ <- partnershipStore.registerPartner(partner)
  } yield (userId, partnerId)

  override def partnerById(partnerId: PartnerId): IO[Option[Partner]] =
    partnershipStore.partnerById(partnerId)

  override def partnerByUserId(userId: UserId): IO[Option[Partner]] =
    partnershipStore.partnerByUserId(userId)

  override def enablePartner(partnerId: PartnerId): IO[Unit] =
    partnershipStore.enablePartner(partnerId, true, clock.now())

  override def disablePartner(partnerId: PartnerId): IO[Unit] =
    partnershipStore.enablePartner(partnerId, false, clock.now())

  override def isPartnerEnabled(partnerId: PartnerId): IO[Boolean] =
    partnershipStore.isPartnerEnabled(partnerId)

  override def partnerFor(postId: PostId): IO[Option[PartnerId]] =
    partnershipStore.partnerFor(postId).map(_.map(_.partnerId))

  override def partnership(partnerId: PartnerId,
                           postId: PostId,
                           from: UTCTimestamp,
                           to: UTCTimestamp): IO[PartnerPost] = for {
    _ <- IOUtils.check(from.value < to.value, "Invalid partnership dates")
    now = clock.now()
    _ <- IOUtils.check(to.value > now.value, "Partnership must end in the future")
    partnerPost = PartnerPost(partnerId, postId, from, to)
    _ <- partnershipStore.registerPartnership(partnerPost)
  } yield partnerPost

  override def hasPartner(postId: PostId): IO[Boolean] =
    partnershipStore.partnerFor(postId).map(_.isDefined)

  override def isPartnershipActive(postId: PostId): IO[Boolean] = (for {
    partnerPost <- OptionT(partnershipStore.partnerFor(postId))
    enabled     <- OptionT.liftF(isPartnerEnabled(partnerPost.partnerId))
    dateValid   <- OptionT.pure[IO](partnerPost.to.value > clock.now().value)
  } yield enabled && dateValid).getOrElse(false)

  override def postShown(postId: PostId, userId: UserId): IO[Unit] = ???

  override def visited(postId: PostId, userId: UserId): IO[Unit] = ???

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkNewPartner(name: String, description: String, url: Url): IO[Unit] = for {
    _ <- IOUtils.check(name.trim.nonEmpty, s"Partner ${name} is already registered")
    _ <- IOUtils.check(description.trim.nonEmpty, s"Partner ${name} has an empty description")
    _ <- IOUtils.check(url.url.trim.nonEmpty, s"URL for partner ${name} cannot be empty")
  } yield ()

}
