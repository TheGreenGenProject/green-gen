package org.greengen.store.user

import cats.effect.IO
import org.greengen.core.{Hash, Page}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}
import org.greengen.store.Store


trait UserStore[F[_]] extends Store[F] {

  def register(userId: UserId,
               userHash: Hash,
               pwHash: Hash,
               user: User,
               profile: Profile): F[()]

  def updateProfile(userId: UserId, profile: Profile): F[Unit]
  def setUserEnabled(userId: UserId, enabled: Boolean): F[Unit]
  def getByUserId(userId: UserId): F[Option[(User, Profile)]]
  def getByUserIds(userIds: List[UserId]): F[List[(User, Profile)]]

  def emailExists(emailHash: Hash): F[Boolean]
  def getByHashes(hashes: (Hash, Hash)): F[Option[UserId]]

  def getByPseudo(pseudo: Pseudo): F[Option[UserId]]
  def getByPseudoPrefix(prefix: String, page: Page): F[List[UserId]]
  def pseudoExists(pseudo: Pseudo): F[Boolean]

  def checkUser(id: UserId): IO[Unit]
  def deleteUser(userId: UserId): F[Option[(User, Profile)]]
  def allUserIds(): F[List[UserId]]
  def activeUsers(): F[List[UserId]]

}

