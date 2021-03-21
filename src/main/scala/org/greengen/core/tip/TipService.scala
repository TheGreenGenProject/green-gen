package org.greengen.core.tip

import org.greengen.core.Source
import org.greengen.core.user.UserId

trait TipService[F[_]] {

  def create(author: UserId, content: String, sources: List[Source]): F[TipId]

  def byId(tipId: TipId): F[Option[Tip]]

  def byAuthor(author: UserId): F[Set[TipId]]

}
