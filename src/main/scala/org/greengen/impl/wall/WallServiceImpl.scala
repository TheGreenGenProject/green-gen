package org.greengen.impl.wall

import cats.effect.IO
import org.greengen.core.post.PostId
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.wall.{Wall, WallService}
import org.greengen.core.{IOUtils, Page}
import org.greengen.store.wall.WallStore


class WallServiceImpl(wallStore: WallStore[IO])
                     (userService: UserService[IO])
  extends WallService[IO] {

  override def wall(userId: UserId, page: Page): IO[Wall] = for {
    _       <- checkUser(userId)
    content <- wallStore.getByUserId(userId, page)
  } yield Wall(userId, content)

  override def addToWall(userId: UserId, postId: PostId): IO[Unit] = for {
    _ <- checkUser(userId)
    _ <- wallStore.addPost(userId, postId)
  } yield ()


  // Helpers

  private[this] def checkUser(id: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(id)
    _ <- IOUtils.check(enabled, s"User $id is not enabled")
  } yield ()

}
