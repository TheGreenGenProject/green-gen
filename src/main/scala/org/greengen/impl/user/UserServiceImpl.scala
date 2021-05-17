package org.greengen.impl.user

import cats.data.OptionT
import cats.effect.IO
import org.greengen.core.user._
import org.greengen.core.{Clock, Hash}
import org.greengen.store.user.UserStore


class UserServiceImpl(userStore: UserStore[IO])(clock: Clock) extends UserService[IO] {

  override def create(pseudo: String,
                      emailHash: Hash,
                      pwHash: Hash,
                      introduction: String): IO[(User,Profile)] = for {
    _ <- checkNewPseudo(pseudo)
    id = UserId.newId()
    user = User(id, emailHash, pwHash, true)
    profile = Profile(id, Pseudo(pseudo), clock.now(), Some(introduction), false)
    _ <- userStore.register(id, emailHash, pwHash, user, profile)
  } yield (user, profile)

  override def delete(id: UserId): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.deleteUser(id)
  } yield ()


  override def updateProfile(id: UserId, profile: Profile): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.updateWith(id){
      case Some((user,_)) => Some((user,profile))
      case _ => None
    }
  } yield ()

  override def enable(id: UserId, reason: String): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.updateWith(id) {
      case Some((user,profile)) =>
        println(s"Enabling user $id. reason: $reason")
        Some((user.copy(enabled = true), profile))
      case _ => None
    }
  } yield ()

  override def disable(id: UserId, reason: String): IO[Unit] = for {
    _ <- userStore.checkUser(id)
    _ <- userStore.updateWith(id) {
      case Some((user,profile)) =>
        println(s"Enabling user $id. reason: $reason")
        Some((user.copy(enabled = false), profile))
      case _ => None
    }
  } yield ()

  override def isEnabled(id: UserId): IO[Boolean] = for {
    maybeUser <- userStore.getByUserId(id)
  } yield maybeUser.exists(_._1.enabled)

  override def byId(id: UserId): IO[Option[(User, Profile)]] =
    userStore.getByUserId(id)

  override def byPseudo(pseudo: Pseudo): IO[Option[UserId]] =
    userStore.getByPseudo(pseudo)

  override def byPseudoPrefix(prefix: String): IO[List[UserId]] =
    userStore.getByPseudoPrefix(prefix)

  override def profile(id: UserId): IO[Option[Profile]] = for {
    maybeUser <- userStore.getByUserId(id)
    res       <- IO(maybeUser.map(_._2))
  } yield res

  override def byHash(email: Hash, pwHash: Hash): IO[Option[(User, Profile)]] = (for {
    maybeId   <- OptionT(userStore.getByHashes((email, pwHash)))
    maybeUser <- OptionT(userStore.getByUserId(maybeId))
  } yield maybeUser).value

  override def users(): IO[List[UserId]] =
    userStore.allUserIds()

  override def activeUsers(): IO[List[UserId]] =
    userStore.activeUsers()

  val PseudoRE = "([a-zA-Z][a-zA-Z0-9_]+)".r
  val PseudoMaxSize = 15
  private[this] def checkNewPseudo(pseudo: String): IO[Unit] = pseudo match {
    case PseudoRE(validated) if validated.size <= PseudoMaxSize =>
      userStore.pseudoExists(Pseudo(validated))
        .flatMap(IO.raiseWhen(_)(new RuntimeException(s"Pseudo $validated already exists. Please try a different one.")))
    case _ =>
      IO.raiseError(new RuntimeException(
        s"Invalid pseudo. A Pseudo must start with a letter, can only contain letters, digits and underscores and be less than $PseudoMaxSize characters"))
  }

}
