package org.greengen.core.registration

import org.greengen.core.Hash
import org.greengen.core.user.Pseudo

trait RegistrationService[F[_]] {

  def register(pseudo: Pseudo,
               emailHash: Hash,
               pwHash: Hash,
               introduction: String): F[Unit]

  def activateUser(emailHash: Hash, validation: ValidationCode): F[Unit]

  def generateNewCode(emailHash: Hash): F[Unit]

  def checkPseudoAvailable(pseudo: Pseudo): F[Boolean]

}
