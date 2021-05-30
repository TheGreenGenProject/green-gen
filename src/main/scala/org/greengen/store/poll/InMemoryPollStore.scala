package org.greengen.store.poll

import cats.effect.IO
import org.greengen.core.{Clock, Page, PagedResult, UTCTimestamp}
import org.greengen.core.poll.{Poll, PollAnswer, PollId, PollStats, PollStatsEntry}
import org.greengen.core.user.UserId

import scala.collection.concurrent.TrieMap


class InMemoryPollStore(clock: Clock) extends PollStore[IO] {

  private[this] val polls = new TrieMap[PollId, Poll]()
  private[this] val authors = new TrieMap[UserId, Set[PollId]]()
  private[this] val answers = new TrieMap[PollId, List[PollAnswer]]()
  private[this] val closed = new TrieMap[PollId, UTCTimestamp]()


  override def register(poll: Poll): IO[Unit] = for {
    _ <- IO(polls.put(poll.id, poll))
    _ <- indexByAuthor(poll)
  } yield ()

  override def getById(pollId: PollId): IO[Option[Poll]] =
    IO(polls.get(pollId))

  override def getByAuthor(author: UserId, page: Page): IO[List[PollId]] = for {
    all    <- IO(authors.getOrElse(author, Set()))
    sorted <- IO(all.toList.sortBy(id => polls.get(id).map(- _.timestamp.value).getOrElse(0L)))
    res    <- IO(PagedResult.page(sorted, page))
  } yield res

  override def isClosed(pollId: PollId): IO[Boolean] =
    IO(closed.contains(pollId))

  override def close(pollId: PollId): IO[Unit] =
    IO(closed.put(pollId, clock.now()))

  override def answer(pollId: PollId, answer: PollAnswer): IO[Unit] =
    addAnswer(pollId, answer)

  override def getAnswer(pollId: PollId, userId: UserId): IO[Option[PollAnswer]] =
    IO(answers.getOrElse(pollId, List()).find(_.userId == userId))

  override def hasResponded(pollId: PollId, userId: UserId): IO[Boolean] =
    getAnswer(pollId, userId).map(_.isDefined)

  override def countRespondents(pollId: PollId): IO[Long] =
    IO(answers.getOrElse(pollId, List()).size)

  override def getStatistics(pollId: PollId): IO[PollStats] = IO {
    PollStats(pollId, answers.getOrElse(pollId, List())
      .groupBy(_.answer)
      .view.mapValues(_.size.toLong)
      .toList
      .map { case (opt, count) => PollStatsEntry(opt, count)})
  }

  // Helpers

  private[this] def indexByAuthor(poll: Poll): IO[Unit] = IO {
    authors.updateWith(poll.author) {
      case Some(ids) => Some(ids + poll.id)
      case None => Some(Set(poll.id))
    }
  }

  private[this] def addAnswer(pollId: PollId, pollAnswer: PollAnswer): IO[Unit] = IO {
    answers.updateWith(pollId) {
      case Some(answers) => Some(pollAnswer :: answers.filter(_.userId != pollAnswer.userId))
      case None => Some(List(pollAnswer))
    }
  }

}
