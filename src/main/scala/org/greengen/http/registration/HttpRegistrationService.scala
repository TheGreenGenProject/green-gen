package org.greengen.http.registration

import cats.effect.IO
import io.circe.syntax._
import org.greengen.core.registration.RegistrationService
import org.greengen.core.user.Pseudo
import org.greengen.http.HttpQueryParameters._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object HttpRegistrationService {

  def nonAuthRoutes(service: RegistrationService[IO]) = HttpRoutes.of[IO] {
    // GET
    case GET -> Root / "registration" / "check-availability" / "pseudo" / pseudo =>
      service.checkPseudoAvailable(Pseudo(pseudo)).flatMap(r => Ok(r.asJson)) // FIXME check pseudo syntax
    // POST
    case POST -> Root / "registration" / "new" :?
      PseudoQueryParamMatcher(pseudo) +&
      EmailHashQueryParamMatcher(emailHash) +&
      PasswordHashQueryParamMatcher(pwHash) +&
      ProfileIntroductionQueryParamMatcher(intro) =>
      service.register(pseudo, emailHash, pwHash, intro).flatMap(r => Ok(r.asJson))
    case POST -> Root / "validation" / "validate" :?
      EmailHashQueryParamMatcher(emailHash) +&
      ValidationCodeQueryParamMatcher(validation) =>
      service.activateUser(emailHash, validation).flatMap(r => Ok(r.asJson))
    case POST -> Root / "validation" / "new" / "code" :?
      EmailHashQueryParamMatcher(emailHash) =>
      service.generateNewCode(emailHash).flatMap(r => Ok(r.asJson))
  }

}
