package org.greengen.store.registration

import org.greengen.core.{Hash, UTCTimestamp}
import org.greengen.core.registration.ValidationCode
import org.greengen.core.user.Pseudo


trait RegistrationStore[F[_]] {

  def emailExists(email: Hash): F[Boolean]

  def register(
    vc: ValidationCode,
    expiry: UTCTimestamp,
    pseudo: Pseudo,
    email: Hash,
    password: Hash,
    introduction: String): F[Unit]

  def validate(validationCode: ValidationCode, email: Hash): F[Option[(Pseudo, Hash, Hash, String)]]

  def remap(email: Hash, newCode: ValidationCode, newExpiry: UTCTimestamp): F[Unit]

  def remove(email: Hash): F[Unit]

  def isExpired(email: Hash): F[Boolean]

}
