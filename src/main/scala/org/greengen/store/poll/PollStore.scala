package org.greengen.store.poll

import cats.effect.IO
import org.greengen.core.Page
import org.greengen.core.poll.{Poll, PollAnswer, PollId, PollStats}
import org.greengen.core.user.UserId



trait PollStore[F[_]] {

  def register(poll: Poll): F[Unit]

  def getById(pollId: PollId): F[Option[Poll]]

  def getByAuthor(author: UserId, page: Page): F[List[PollId]]

  def isClosed(pollId: PollId): F[Boolean]

  def close(pollId: PollId): F[Unit]

  def answer(pollId: PollId, answer: PollAnswer): F[Unit]

  def getAnswer(pollId: PollId, userId: UserId): F[Option[PollAnswer]]

  def hasResponded(pollId: PollId, userId: UserId): F[Boolean]

  def countRespondents(pollId: PollId): IO[Long]

  def getStatistics(pollId: PollId): IO[PollStats]

}
