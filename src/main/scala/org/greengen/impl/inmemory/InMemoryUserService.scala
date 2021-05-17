package org.greengen.impl.inmemory

import cats.effect.IO
import org.greengen.core.user.{Profile, Pseudo, User, UserId, UserService}
import org.greengen.core.{Clock, Hash, UTCTimestamp}

import scala.collection.concurrent.TrieMap


@deprecated
class InMemoryUserService(clock: Clock) extends UserService[IO] {

  private[this] val allUsers = new TrieMap[UserId,(User, Profile)]
  private[this] val byHashes = new TrieMap[(Hash, Hash),UserId]
  private[this] val byPseudos = new TrieMap[Pseudo, UserId] // Pseudos are unique
  // TODO Trie for pseudo prefix indexing for fast pseudo prefix search ?

  override def create(pseudo: String,
                      emailHash: Hash,
                      pwHash: Hash,
                      introduction: String): IO[(User,Profile)] =
    for {
      _ <- checkNewPseudo(pseudo)
    } yield {
      val id = UserId.newId()
      val user = User(id, emailHash, pwHash, true)
      val profile = Profile(id, Pseudo(pseudo), clock.now(), Some(introduction), false)
      allUsers.put(id, (user, profile))
      byHashes.put((emailHash, pwHash), id)
      byPseudos.put(profile.pseudo, id) // FIXME possible race condition here
      (user, profile)
    }

  override def delete(id: UserId): IO[Unit] = IO {
    checkUser(id)
    allUsers.remove(id)
      .foreach { case (user, profile) =>
        byHashes.remove((user.emailHash, user.passwordHash))
        byPseudos.remove(profile.pseudo)
      }
  }

  override def updateProfile(id: UserId, profile: Profile): IO[Unit] = IO {
    checkUser(id)
    allUsers.updateWith(id) {
      case Some((user,_)) => Some((user,profile))
      case _ => None
    }
  }

  override def enable(id: UserId, reason: String): IO[Unit] = IO {
    checkUser(id)
    allUsers.updateWith(id) {
      case Some((user,profile)) =>
        println(s"Enabling user $id. reason: $reason")
        Some((user.copy(enabled = true), profile))
      case _ => None
    }
  }

  override def disable(id: UserId, reason: String): IO[Unit] = IO {
    checkUser(id)
    allUsers.updateWith(id) {
      case Some((user,profile)) =>
        println(s"Enabling user $id. reason: $reason")
        Some((user.copy(enabled = false), profile))
      case _ => None
    }
  }

  override def isEnabled(id: UserId): IO[Boolean] = IO {
    allUsers.get(id).exists(_._1.enabled)
  }

  override def byId(id: UserId): IO[Option[(User, Profile)]] = IO {
    allUsers.get(id)
  }

  override def byPseudo(pseudo: Pseudo): IO[Option[UserId]] = IO {
    byPseudos.get(pseudo)
  }

  // FIXME optimize that ...
  override def byPseudoPrefix(prefix: String): IO[List[UserId]] = IO {
    byPseudos.collect { case (Pseudo(pseudo), id)
      if pseudo.toLowerCase.startsWith(prefix.toLowerCase) => id
    }.toList
  }

  override def profile(id: UserId): IO[Option[Profile]] = IO {
    allUsers.get(id).map(_._2)
  }

  override def byHash(email: Hash, pwHash: Hash): IO[Option[(User, Profile)]] =
    byHashes.get((email,pwHash))
      .fold(IO.pure[Option[(User, Profile)]](None))(byId(_))

  override def users(): IO[List[UserId]] = IO {
    allUsers.keys.toList
  }

  override def activeUsers(): IO[List[UserId]] = IO {
    allUsers
      .collect { case (id,(user, _)) if user.enabled => id }
      .toList
  }

  private[this] def checkUser(id: UserId): Unit =
    if(!allUsers.contains(id))
      throw new RuntimeException(s"Unknown user id: $id")

  val PseudoRE = "([a-zA-Z][a-zA-Z0-9_]+)".r
  val PseudoMaxSize = 15
  private[this] def checkNewPseudo(pseudo: String): IO[Unit] = pseudo match {
    case PseudoRE(validated) if validated.size <= PseudoMaxSize =>
      IO.raiseWhen(byPseudos.contains(Pseudo(validated)))(new RuntimeException(s"Pseudo $validated already exists. Pleas etry a different one."))
    case _ =>
      IO.raiseError(new RuntimeException(
        s"Invalid pseudo. A Pseudo must start with a letter, can only contain letters, digits and underscores and be less than $PseudoMaxSize characters"))
  }

}
