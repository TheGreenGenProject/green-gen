package org.greengen.store

import org.greengen.core.UUID
import org.greengen.core.challenge.Challenge

trait ChallengeStore[F[_]] {

  def newChallenge(): F[Challenge]

  def addParticipant(challengeId: UUID, user: UUID): F[Boolean]

  def remove(challengeId: UUID): F[Boolean]

}
