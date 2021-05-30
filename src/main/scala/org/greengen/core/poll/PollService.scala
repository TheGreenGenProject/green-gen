package org.greengen.core.poll

import org.greengen.core.Page
import org.greengen.core.user.UserId


trait PollService[F[_]] {

  def create(author: UserId,
             title: String,
             options: List[PollOption]): F[PollId]

  def byId(pollId: PollId): F[Option[Poll]]

  def byAuthor(author: UserId, page: Page): F[List[PollId]]

  def isClosed(pollId: PollId): F[Boolean]

  def closePoll(author: UserId, pollId: PollId): F[Unit]

  def answer(pollId: PollId, userId: UserId, optionIndex: Int): F[Unit]

  def answer(pollId: PollId, answer: PollAnswer): F[Unit]

  def getAnswerFor(pollId: PollId, userId: UserId): F[Option[PollAnswer]]

  def hasResponded(pollId: PollId, userId: UserId): F[Boolean]

  def respondents(pollId: PollId): F[Long]

  def statisics(pollId: PollId): F[PollStats]

}
