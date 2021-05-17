package org.greengen.impl.tip

import cats.effect.IO
import org.greengen.core.tip.{Tip, TipId, TipService}
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Source}
import org.greengen.store.tip.TipStore


class TipServiceImpl(tipStore: TipStore[IO])
                    (clock: Clock, userService: UserService[IO]) extends TipService[IO] {

  override def create(author: UserId, content: String, sources: List[Source]): IO[TipId] = for {
    _     <- checkUser(author)
    tipId <- IO(TipId.newId())
    tip   <- IO(Tip(tipId, author, content, clock.now(), sources))
    _     <- tipStore.store(tip)
  } yield tipId

  override def byId(tipId: TipId): IO[Option[Tip]] =
    tipStore.getByTipId(tipId)

  override def byAuthor(author: UserId): IO[Set[TipId]] =
    tipStore.getByAuthor(author)

  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _ <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

}
