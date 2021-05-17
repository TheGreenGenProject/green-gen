package org.greengen.store.tip

import org.greengen.core.tip.{Tip, TipId}
import org.greengen.core.user.UserId
import org.greengen.store.Store

trait TipStore[F[_]] extends Store[F] {

  def getByTipId(id: TipId): F[Option[Tip]]

  def getByAuthor(id: UserId): F[Set[TipId]]

  def store(tip: Tip): F[Unit]

}
