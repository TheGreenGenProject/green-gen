package org.greengen.store.user

import cats.implicits._
import cats.effect.IO
import org.greengen.core.{Hash, Page}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}

object CachedUserStore {
  def withCache(store: UserStore[IO]): UserStore[IO] =
    new CachedUserStore(new InMemoryUserStore, store)
}


private[user] class CachedUserStore(cache: UserStore[IO], persistent: UserStore[IO]) extends UserStore[IO] {

  override def register(userId: UserId, userHash: Hash, pwHash: Hash, user: User, profile: Profile): IO[Unit] = for {
    _ <- persistent.register(userId,userHash, pwHash, user, profile)
    _ <- cache.register(userId,userHash, pwHash, user, profile)
  } yield ()

  override def updateProfile(userId: UserId, profile: Profile): IO[Unit] = for {
    _ <- persistent.updateProfile(userId, profile)
    _ <- cache.updateProfile(userId, profile)
  } yield ()

  override def setUserEnabled(userId: UserId, enabled: Boolean): IO[Unit] = for {
    _ <- persistent.setUserEnabled(userId, enabled)
    _ <- cache.setUserEnabled(userId, enabled)
  } yield ()

  override def getByUserId(userId: UserId): IO[Option[(User, Profile)]] = for {
    maybeUser <- cache.getByUserId(userId)
    result    <- maybeUser.fold(persistent.getByUserId(userId))(p => IO(Some(p)))
    _         <- (maybeUser, result) match {
        case (None, Some((user, profile))) => cacheUser(user, profile)
        case _                             => IO.unit
    }
  } yield result

  override def getByUserIds(userIds: List[UserId]): IO[List[(User, Profile)]] = for {
    cachedUsers <- cache.getByUserIds(userIds)
    foundIds = cachedUsers.map(_._1.id).toSet
    notFound = userIds.filterNot(foundIds(_))
    notYetCached <- persistent.getByUserIds(notFound)
    result    <- IO(cachedUsers ++ notYetCached)
    _         <- notYetCached match {
      case xs if xs.nonEmpty => xs.map { case (user, profile) => cacheUser(user, profile) }.sequence
      case _                 => IO.unit
    }
  } yield result

  override def emailExists(emailHash: Hash): IO[Boolean] =
    persistent.emailExists(emailHash)

  override def getByHashes(hashes: (Hash, Hash)): IO[Option[UserId]] =
    persistent.getByHashes(hashes)

  override def getByPseudo(pseudo: Pseudo): IO[Option[UserId]] = for {
    maybeUser <- cache.getByPseudo(pseudo)
    result    <- maybeUser.fold(persistent.getByPseudo(pseudo))(p => IO(Some(p)))
    _         <- (maybeUser, result) match {
        case (None, Some(userId)) => cacheUserId(userId)
        case _                    => IO.unit
      }
  } yield result

  override def getByPseudoPrefix(prefix: String, page: Page): IO[List[UserId]] =
    persistent.getByPseudoPrefix(prefix, page)

  override def pseudoExists(pseudo: Pseudo): IO[Boolean] =
    persistent.pseudoExists(pseudo)

  override def checkUser(id: UserId): IO[Unit] =
    persistent.checkUser(id)

  override def deleteUser(userId: UserId): IO[Option[(User, Profile)]] = for {
    res <- persistent.deleteUser(userId)
    _   <- cache.deleteUser(userId)
  } yield res

  override def allUserIds(): IO[List[UserId]] =
    persistent.allUserIds()

  override def activeUsers(): IO[List[UserId]] =
    persistent.activeUsers()


  // Helpers

  private[this] def cacheUserId(userId: UserId): IO[Unit] = for {
    userData <- persistent.getByUserId(userId)
    _        <- userData.map { case (user, profile) =>
      cacheUser(user, profile) }.getOrElse(IO.unit)
  } yield ()

  // Feed in-memory cache
  private[this] def cacheUser(user: User, profile: Profile): IO[Unit] =
    cache.register(user.id, user.emailHash, user.passwordHash, user, profile)

}
