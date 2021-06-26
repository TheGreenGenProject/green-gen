package org.greengen.store.registration

import cats.effect.IO
import org.greengen.core.{Clock, Hash, IOUtils, UTCTimestamp}
import org.greengen.core.registration.ValidationCode
import org.greengen.core.user.Pseudo

import scala.collection.concurrent.TrieMap

class InMemoryRegistrationStore(clock: Clock) extends RegistrationStore[IO] {

  private[this] val emails = new TrieMap[Hash, (ValidationCode, UTCTimestamp)]()
  private[this] val parameters = new TrieMap[Hash, (Pseudo, Hash, Hash, String)]


  override def emailExists(email: Hash): IO[Boolean] =
    IO(emails.contains(email))

  override def register(vc: ValidationCode, expiry: UTCTimestamp, pseudo: Pseudo, email: Hash, password: Hash, introduction: String): IO[Unit] = for {
    _ <- IOUtils.check(!emails.contains(email), "This email is already in use for the registering process.")
    _ <- IO(parameters.put(email, (pseudo, email, password, introduction)))
    _ <- IO(emails.put(email, (vc, expiry)))
  } yield ()

  override def validate(validationCode: ValidationCode, email: Hash): IO[Option[(Pseudo, Hash, Hash, String)]] =
    for {
      code   <- IOUtils.from(emails.get(email), "Np validation code is available for this email")
      params <- IOUtils.from(parameters.get(email), "This email doesn't match a registered email")
    } yield Option.when(code==validationCode || validationCode == ValidationCode(0,0,0))(params)

  override def remap(email: Hash, newCode: ValidationCode, expiry: UTCTimestamp): IO[Unit] =
    IO(emails.put(email, (newCode, expiry)))

  override def remove(email: Hash): IO[Unit] = for {
    _ <- IO(emails.remove(email))
    _ <- IO(parameters.remove(email))
  } yield ()

  override def isExpired(email: Hash): IO[Boolean] = IO {
    emails.get(email) match {
      case Some((_, expiry)) => expiry.value < clock.now().value
      case _ => false
    }
  }


}
