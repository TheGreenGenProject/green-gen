package org.greengen.impl.registration

import cats.effect.IO
import org.greengen.core.registration.{RegistrationService, ValidationCode}
import org.greengen.core.user.{Pseudo, UserService}
import org.greengen.core.{Clock, Hash, IOUtils}
import org.greengen.store.registration.RegistrationStore


class RegistrationServiceImpl(registrationStore: RegistrationStore[IO])
                             (clock: Clock, userService: UserService[IO]) extends RegistrationService[IO] {

  val RegistrationCodeValidity = 24 * 3600 * 1000
  val maxIntroSize = 5000

  override def register(pseudo: Pseudo,
                        emailHash: Hash,
                        pwHash: Hash,
                        introduction: String): IO[Unit] = for {
    _    <- checkPseudo(pseudo)
    _    <- checkEmail(emailHash)
    _    <- checkPassword(emailHash, pwHash)
    _    <- checkIntroduction(introduction)
    _    <- checkRegisteringEmail(emailHash)
    vc   <- generateValidationCode()
    expiry = clock.now().plusMillis(RegistrationCodeValidity)
    _    <- registrationStore.register(vc, expiry, pseudo, emailHash, pwHash, introduction)
  } yield ()

  override def activateUser(email: Hash, validation: ValidationCode): IO[Unit] = for {
    expired               <- registrationStore.emailExists(email)
    _                     <- IOUtils.check(expired, s"Activation code ${ValidationCode.format(validation)} is expired")
    maybeRegistrationData <- registrationStore.validate(validation , email)
    validToken            <- IOUtils.from(maybeRegistrationData, s"Invalid activation code ${ValidationCode.format(validation)}")
    (pseudo, email, pw, intro) = validToken
    // Just to make sure there is no other user with the same pseudo / email at activation time
    // TODO atomic checks
    _    <- checkPseudo(pseudo)
    _    <- checkEmail(email)
    _    <- registrationStore.remove(email)
    user <- userService.create(pseudo, email, pw, intro)
    _    <- userService.enable(user._1.id, s"User validation created: ${user._1.id}")
  } yield ()

  override def generateNewCode(email: Hash): IO[Unit] = for {
    _      <- checkEmail(email)
    exists <- registrationStore.emailExists(email)
    _      <- IOUtils.check(exists, "Email not found. Please try to register again")
    vc     <- generateValidationCode()
    _      <- registrationStore.remap(email, vc, clock.now().plusMillis(RegistrationCodeValidity))
  } yield ()

  override def checkPseudoAvailable(pseudo: Pseudo): IO[Boolean] = for {
    maybePseudo <- userService.byPseudo(pseudo)
    available   <- IO(maybePseudo.isEmpty)
  } yield available

  // Helpers

  private[this] def generateValidationCode(): IO[ValidationCode] =
    IO(ValidationCode.generate())

  // Checkers

  private[this] def checkPseudo(pseudo: Pseudo): IO[Unit] = for {
    _ <- userService.byPseudo(pseudo)
  } yield ()

  private[this] def checkEmail(emailHash: Hash): IO[Unit] = for {
    exists <- userService.emailExists(emailHash)
    _ <- IOUtils.check(!exists, "This email is already registered")
  } yield ()

  private[this] def checkRegisteringEmail(emailHash: Hash): IO[Unit] = for {
    exists <- registrationStore.emailExists(emailHash)
    _ <- IOUtils.check(!exists,
      "This email is already being processed for registration. Please activate your account by using the validation code we sent to your email.")
  } yield ()

  private[this] def checkIntroduction(introduction: String): IO[Unit] = for {
    _ <- IOUtils.check(introduction.trim.nonEmpty, "Introduction cannot be empty.")
    _ <- IOUtils.check(introduction.size < maxIntroSize, s"Introduction is too long. (max ${maxIntroSize} chararcters)")
  } yield ()

  private[this] def checkPassword(emailHash: Hash, pwHash: Hash): IO[Unit] = for {
    _ <- IOUtils.check(emailHash != pwHash, "You cannot use your email as a password")
  } yield ()

}
