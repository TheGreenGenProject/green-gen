package org.greengen.store.user

import cats.data.OptionT
import cats.effect.IO
import org.greengen.core.{Hash, Page, PagedResult}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}

import scala.collection.concurrent.TrieMap


class InMemoryUserStore extends UserStore[IO] {

  private[this] val allUsers = new TrieMap[UserId,(User, Profile)]
  private[this] val byHashes = new TrieMap[(Hash, Hash),UserId]
  private[this] val emailHashes = new TrieMap[Hash, UserId]
  private[this] val byPseudos = new TrieMap[Pseudo, UserId] // Pseudos are unique


  override def register(userId: UserId, emailHash: Hash, pwHash: Hash, user: User, profile: Profile): IO[Unit] = for {
    _ <- IO(allUsers.put(userId, (user, profile)))
    _ <- IO(byHashes.put((emailHash, pwHash), userId))
    _ <- IO(emailHashes.put(emailHash, userId))
    _ <- IO(byPseudos.put(profile.pseudo, userId))
  } yield ()

  override def updateProfile(userId: UserId, profile: Profile): IO[Unit] =
    for {
      _ <- checkUser(userId)
      _ <- updateWith(userId) {
        case Some((user, _)) => Some((user, profile))
        case _ => None
      }
    } yield ()

  override def setUserEnabled(userId: UserId, enabled: Boolean): IO[Unit] =
    for {
      _ <- checkUser(userId)
      _ <- updateWith(userId) {
        case Some((user, profile)) => Some((user.copy(enabled = enabled), profile))
        case _ => None
      }
    } yield ()

  override def getByUserId(userId: UserId): IO[Option[(User, Profile)]] =
    IO(allUsers.get(userId))

  override def emailExists(emailHash: Hash): IO[Boolean] =
    IO(emailHashes.contains(emailHash))

  override def getByHashes(hashes: (Hash, Hash)): IO[Option[UserId]] =
    IO(byHashes.get(hashes))

  override def getByPseudo(pseudo: Pseudo): IO[Option[UserId]] =
    IO(byPseudos.get(pseudo))

  override def getByPseudoPrefix(prefix: String, page: Page): IO[List[UserId]] = IO {
    PagedResult.page(byPseudos.collect { case (Pseudo(pseudo), id)
      if pseudo.toLowerCase.startsWith(prefix.toLowerCase) => id
    }.toList, page)
  }

  override def pseudoExists(pseudo: Pseudo): IO[Boolean] =
    IO(byPseudos.contains(pseudo))

  override def checkUser(id: UserId): IO[Unit] =
    IO.raiseWhen(!allUsers.contains(id))(new RuntimeException(s"Unknown user id: $id"))

  override def deleteUser(userId: UserId): IO[Option[(User, Profile)]] = (for {
    _               <- OptionT.liftF(checkUser(userId))
    (user, profile) <- OptionT.fromOption[IO](allUsers.remove(userId))
    _               <- OptionT.fromOption[IO](byHashes.remove((user.emailHash, user.passwordHash)))
    _               <- OptionT.fromOption[IO](byPseudos.remove(profile.pseudo))
  } yield (user, profile)).value

  override def allUserIds(): IO[List[UserId]] =
    IO(allUsers.keys.toList)

  override def activeUsers(): IO[List[UserId]] = IO {
    allUsers
      .collect { case (id,(user, _)) if user.enabled => id }
      .toList
  }

  // Helpers

  private[this] def updateWith(userId: UserId)(f: Option[(User, Profile)] => Option[(User, Profile)]): IO[Unit] =
    IO(allUsers.updateWith(userId)(f))

}
