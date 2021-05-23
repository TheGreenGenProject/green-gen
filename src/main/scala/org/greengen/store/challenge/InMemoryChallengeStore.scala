package org.greengen.store.challenge

import cats.effect.IO
import cats.implicits._
import org.greengen.core.challenge.{Challenge, ChallengeId, ChallengeStepReportEntry, ChallengeStepReportStatus}
import org.greengen.core.user.UserId
import org.greengen.core.{Page, PagedResult}

import scala.collection.concurrent.TrieMap


class InMemoryChallengeStore extends ChallengeStore[IO] {

  val RejectedChallenge = Left(())

  private[this] val challenges = new TrieMap[ChallengeId, Challenge]
  private[this] val authors    = new TrieMap[UserId, Set[ChallengeId]]
  private[this] val challenged = new TrieMap[ChallengeId, Set[UserId]]
  private[this] val results    = new TrieMap[(UserId, ChallengeId), Either[Unit, List[ChallengeStepReportEntry]]]


  override def register(challenge: Challenge): IO[Unit] = for {
    _ <- indexById(challenge)
    _ <- indexByAuthor(challenge)
  } yield ()

  override def getById(challengeId: ChallengeId): IO[Option[Challenge]] =
    IO(challenges.get(challengeId))

  override def getByAuthor(userId: UserId): IO[List[ChallengeId]] =
    IO(authors.get(userId).map(_.toList).getOrElse(List()))

  override def getByAuthor(author: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] = for {
    asAuthor <- getByAuthor(author)
    filtered <- filterChallengeIds(asAuthor, predicate)
    sorted   <- sortChallengeIds(filtered)
    res      <- IO(PagedResult.page(sorted, page))
  } yield res

  override def getByUser(userId: UserId): IO[List[ChallengeId]] =
    getByUser(userId, Page.All, _ => true)

  override def getByUser(userId: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] = for {
    asAuthor     <- getByAuthor(userId)
    asChallengee <- IO(challenged.collect { case (challengeId, challengees) if challengees.contains(userId) => challengeId }.toList)
    indep        <- IO(results.keys.collect { case (contestant, challengeId) if contestant==userId => challengeId }.toList)
    filtered     <- filterChallengeIds((asChallengee ++ indep ++ asAuthor).distinct, predicate)
    sorted       <- sortChallengeIds(filtered)
    res          <- IO(PagedResult.page(sorted, page))
  } yield res

  override def getResultsByChallenge(challengeId: ChallengeId): IO[List[Either[Unit, List[ChallengeStepReportEntry]]]] =
    IO(results.collect { case ((_, id), value) if id == challengeId => value}.toList)

  override def isChallengeAccepted(challengeId: ChallengeId, byUser: UserId): IO[Boolean] =
    IO(results.getOrElse((byUser, challengeId), RejectedChallenge).isRight)

  override def isChallengeRejected(challengeId: ChallengeId, byUser: UserId): IO[Boolean] =
    IO(results.getOrElse((byUser, challengeId), Right(List())).isLeft)

  override def acceptChallenge(userId: UserId, challengeId: ChallengeId): IO[Unit] =
    IO(results.putIfAbsent((userId, challengeId), Right(List())))

  override def rejectChallenge(userId: UserId, challengeId: ChallengeId): IO[Unit] =
    IO(results.putIfAbsent((userId, challengeId), RejectedChallenge))

  override def cancelChallenge(userId: UserId, challengeId: ChallengeId): IO[Unit] =
    IO(results.put((userId, challengeId), RejectedChallenge))

  override def getStepReports(userId: UserId, challengeId: ChallengeId): IO[List[ChallengeStepReportEntry]] =
    IO(results.get((userId, challengeId)).flatMap(_.toOption).getOrElse(List()))

  override def reportStep(userId: UserId, challengeId: ChallengeId, step: Int, report: ChallengeStepReportStatus): IO[Unit] =
    IO(results.put((userId, challengeId),
                    results.getOrElse((userId, challengeId), Right(List()))
                      .map { steps =>
                        val filtered = steps.filter(_.step!=step) // making sure we don't already have a report for the given step
                        ChallengeStepReportEntry(step, report) :: filtered
                      }))

  override def getChallengees(challengeId: ChallengeId, page: Page): IO[List[UserId]] =
    IO(challenged.get(challengeId).map(_.toList).getOrElse(List()))

  override def getChallengeeCount(challengeId: ChallengeId): IO[Long] = for {
    allChallengees <- getChallengees(challengeId, Page.All)
  } yield allChallengees.size.toLong

  override def markUserAsChallenged(userId: UserId, challengeId: ChallengeId): IO[Unit] =
    indexChallengeeByChallenge(userId, challengeId)



  // Sorting

  private[this] def sortChallengeIds(challengeIds: List[ChallengeId]): IO[List[ChallengeId]] = for {
    challenges <- challengeIds.map(getById).sequence
    flattened  <- IO(challenges.flatten)
    sorted     <- sortChallenges(flattened)
  } yield sorted.map(_.id)

  private[this] def sortChallenges(challenges: List[Challenge]): IO[List[Challenge]] = IO {
    challenges.sortBy(-1L * _.created.value)
  }

  // Filtering

  private[this] def filterChallengeIds(challengeIds: List[ChallengeId],
                                       p: Challenge => Boolean): IO[List[ChallengeId]] = for {
    challenges <- challengeIds.map(getById).sequence
    flattened  <- IO(challenges.flatten)
    filtered   <- IO(flattened.filter(p))
  } yield filtered.map(_.id)


  // Indexers

  private[this] def indexById(challenge: Challenge): IO[Unit] =
    IO(challenges.put(challenge.id, challenge))

  private[this] def indexByAuthor(challenge: Challenge): IO[Unit] = IO {
    authors.updateWith(challenge.author) {
      case Some(ids) => Some(ids + challenge.id)
      case None => Some(Set(challenge.id))
    }
  }

  private[this] def indexChallengeeByChallenge(challengeeId: UserId, challengeId: ChallengeId): IO[Unit] = IO {
    challenged.updateWith(challengeId) {
      case Some(challengees) => Some(challengees + challengeeId)
      case None => Some(Set(challengeeId))
    }
  }
}
