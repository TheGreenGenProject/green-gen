package org.greengen.impl.poll

import cats.data.OptionT
import cats.effect.IO
import org.greengen.core.poll._
import org.greengen.core.user.{UserId, UserService}
import org.greengen.core.{Clock, IOUtils, Page}
import org.greengen.store.poll.PollStore



class PollServiceImpl(pollStore: PollStore[IO])
                     (clock: Clock,
                      userService: UserService[IO]) extends PollService[IO] {

  override def create(author: UserId,
                      title: String,
                      options: List[PollOption]): IO[PollId] = for {
    _      <- checkUser(author)
    pollId <- IO(PollId.newId())
    poll   <- IO(Poll(pollId, author, title, options, clock.now()))
    _      <- pollStore.register(poll)
  } yield pollId

  override def byId(pollId: PollId): IO[Option[Poll]] =
    pollStore.getById(pollId)

  override def byAuthor(author: UserId, page: Page): IO[List[PollId]] = for {
    _   <- checkUser(author)
    res <- pollStore.getByAuthor(author, page)
  } yield res

  override def isClosed(pollId: PollId): IO[Boolean] =
    pollStore.isClosed(pollId)

  override def closePoll(userId: UserId, pollId: PollId): IO[Unit] = for {
    _   <- checkAuthor(userId, pollId)
    res <- pollStore.close(pollId)
  } yield res

  override def answer(pollId: PollId, userId: UserId, optionIndex: Int): IO[Unit] = (for {
    poll     <- OptionT(pollStore.getById(pollId))
    selected <- OptionT.fromOption[IO](poll.options.drop(optionIndex - 1).headOption)
    res      <- OptionT.liftF(answer(pollId, PollAnswer(userId, selected, clock.now())))
  } yield res).value.map( _ => ())

  override def answer(pollId: PollId, answer: PollAnswer): IO[Unit] = for {
    _   <- checkUser(answer.userId)
    res <- pollStore.answer(pollId, answer)
  } yield res

  override def getAnswerFor(pollId: PollId, userId: UserId): IO[Option[PollAnswer]] =
    pollStore.getAnswer(pollId, userId)

  override def hasResponded(pollId: PollId, userId: UserId): IO[Boolean] =
    pollStore.hasResponded(pollId, userId)

  override def respondents(pollId: PollId): IO[Long] =
    pollStore.countRespondents(pollId)

  override def statisics(pollId: PollId): IO[PollStats] =
    pollStore.getStatistics(pollId)


  // Checkers

  private[this] def checkUser(user: UserId): IO[Unit] = for {
    enabled <- userService.isEnabled(user)
    _       <- IOUtils.check(enabled, s"User $user is disabled")
  } yield ()

  private[this] def checkAuthor(userId: UserId, pollId: PollId): IO[Unit] = for {
    _    <- checkUser(userId)
    poll <- pollStore.getById(pollId)
    _    <- IOUtils.check(poll.exists(_.author==userId), s"User $userId is not the author of poll $pollId")
  } yield ()

}
