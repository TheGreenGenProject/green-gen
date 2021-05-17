package org.greengen.store.wall

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryWallStore extends WallStore[IO] {

  private[this] val walls = new TrieMap[UserId, IndexedSeq[PostId]]

  override def getByUserId(id: UserId): IO[Option[IndexedSeq[PostId]]] =
    IO(walls.get(id))

  override def updateWith(id: UserId)(f: Option[IndexedSeq[PostId]] => Option[IndexedSeq[PostId]]): IO[Unit] =
    IO(walls.updateWith(id)(f))

}
