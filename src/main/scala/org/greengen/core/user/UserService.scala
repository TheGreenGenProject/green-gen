package org.greengen.core.user

import org.greengen.core.{Hash, Page, UTCTimestamp}

trait UserService[F[_]] {

  // Creates a new user
  def create(pseudo: Pseudo, emailHash: Hash, pwHash: Hash, introduction: String): F[(User,Profile)]

  // Delete a user
  def delete(id: UserId): F[Unit]

  def updateProfile(id: UserId, profile: Profile): F[Unit]

  def enable(id: UserId, reason: String): F[Unit]

  def disable(id: UserId, reason: String): F[Unit]

  def isEnabled(id: UserId): F[Boolean]

  def byId(id: UserId): F[Option[(User, Profile)]]

  def byPseudo(pseudo: Pseudo): F[Option[UserId]]

  def byPseudoPrefix(prefix: String, page: Page): F[List[UserId]]

  def profile(id: UserId): F[Option[Profile]]

  def emailExists(email: Hash): F[Boolean]

  def byHash(email: Hash, pwHash: Hash): F[Option[(User, Profile)]]

  // All users from the platform
  def users(): F[List[UserId]]

  // All active users from the platform
  def activeUsers(): F[List[UserId]]

}
