package org.greengen.http.user

import cats.effect.IO
import org.greengen.core.Hash
import org.greengen.core.user.{Profile, Pseudo, User, UserId, UserService}
import org.greengen.http.HttpHelper.{delete => del, _}
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client._


class HttpUserServiceClient(httpClient: Client[IO], root: Uri) extends UserService[IO] {

  override def create(pseudo: String,
                      emailHash: Hash,
                      pwHash: Hash,
                      introduction: String): IO[(User, Profile)] =
    httpClient.expect[(User, Profile)](post(root / "create",
      "pseudo" -> pseudo,
      "email-hash" -> emailHash,
      "password-hash" -> pwHash,
      "introduction" ->introduction))

  override def delete(id: UserId): IO[Unit] =
    httpClient.expect[Unit](del(root / "user" / "delete", "id" -> id.value.uuid))

  override def updateProfile(id: UserId, profile: Profile): IO[Unit] = ???

  override def enable(id: UserId, reason: String): IO[Unit] =
    httpClient.expect[Unit](post(root / "user" / "enable", "id" -> id.value.uuid, "reason" -> reason))

  override def disable(id: UserId, reason: String): IO[Unit] =
    httpClient.expect[Unit](post(root / "user" / "disable", "id" -> id.value.uuid, "reason" -> reason))

  override def isEnabled(id: UserId): IO[Boolean] =
    httpClient.expect[Boolean](root / "user" / "is-enabled" / id.value.uuid)

  override def byId(id: UserId): IO[Option[(User, Profile)]] =
    httpClient.expect[Option[(User, Profile)]](root / "user" / "by-id" / id.value.uuid)

  override def byPseudo(pseudo: Pseudo): IO[Option[UserId]] =
    httpClient.expect[Option[UserId]](root / "user" / "by-pseudo" / pseudo.value)

  override def byPseudoPrefix(prefix: String): IO[List[UserId]] =
    httpClient.expect[List[UserId]](root / "user" / "by-prefix" / prefix)

  override def profile(id: UserId): IO[Option[Profile]] =
    httpClient.expect[Option[Profile]](root / "user" / "profile-by-id" / id.value.uuid)

  override def byHash(email: Hash, pwHash: Hash): IO[Option[(User, Profile)]] =
    throw new RuntimeException("Not queryable")

  override def users(): IO[List[UserId]] =
    httpClient.expect[List[UserId]](root / "users" / "all")

  override def activeUsers(): IO[List[UserId]] =
   httpClient.expect[List[UserId]](root / "users" / "active")

}
