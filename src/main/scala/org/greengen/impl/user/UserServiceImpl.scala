package org.greengen.impl.user

import cats.data.OptionT
import cats.effect.IO
import org.greengen.core.user._
import org.greengen.core.{Clock, Hash, IOUtils, Page}
import org.greengen.store.user.UserStore


class UserServiceImpl(userStore: UserStore[IO])(clock: Clock) extends UserService[IO] {

  override def create(pseudo: Pseudo,
                      emailHash: Hash,
                      pwHash: Hash,
                      introduction: String): IO[(User,Profile)] = for {
    _ <- checkNewPseudo(pseudo)
    id = UserId.newId()
    user = User(id, emailHash, pwHash, true)
    profile = Profile(id, pseudo, clock.now(), Some(introduction), false)
    _ <- userStore.register(id, emailHash, pwHash, user, profile)
  } yield (user, profile)

  override def delete(id: UserId): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.deleteUser(id)
  } yield ()


  override def updateProfile(id: UserId, profile: Profile): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.updateProfile(id, profile)
  } yield ()

  override def enable(id: UserId, reason: String): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.setUserEnabled(id, true)
  } yield ()

  override def disable(id: UserId, reason: String): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.setUserEnabled(id, false)
  } yield ()

  override def isEnabled(id: UserId): IO[Boolean] = for {
    maybeUser <- userStore.getByUserId(id)
  } yield maybeUser.exists(_._1.enabled)

  override def byId(id: UserId): IO[Option[(User, Profile)]] =
    userStore.getByUserId(id)

  override def byPseudo(pseudo: Pseudo): IO[Option[UserId]] =
    userStore.getByPseudo(pseudo)

  override def byPseudoPrefix(prefix: String, page: Page): IO[List[UserId]] =
    userStore.getByPseudoPrefix(prefix, page)

  override def profile(id: UserId): IO[Option[Profile]] = for {
    maybeUser <- userStore.getByUserId(id)
    res       <- IO(maybeUser.map(_._2))
  } yield res

  override def emailExists(email: Hash): IO[Boolean] =
    userStore.emailExists(email)

  override def byHash(email: Hash, pwHash: Hash): IO[Option[(User, Profile)]] = (for {
    maybeId   <- OptionT(userStore.getByHashes((email, pwHash)))
    maybeUser <- OptionT(userStore.getByUserId(maybeId))
  } yield maybeUser).value

  override def users(): IO[List[UserId]] =
    userStore.allUserIds()

  override def activeUsers(): IO[List[UserId]] =
    userStore.activeUsers()

  private[this] def checkNewPseudo(pseudo: Pseudo): IO[Unit] = for {
    exists <- userStore.pseudoExists(pseudo)
    _      <- IOUtils.check(!exists, s"Pseudo ${pseudo.value} already exists. Please try a different one.")
  } yield ()

}
