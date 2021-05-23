package org.greengen.store.challenge

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, in, eq => eql}
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.Updates.{combine, set, setOnInsert}
import org.greengen.core.challenge.{Challenge, ChallengeId, ChallengeStepReportEntry, ChallengeStepReportStatus}
import org.greengen.core.user.UserId
import org.greengen.core.{Clock, Page, UUID}
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.UpdateOptions



class MongoChallengeStore(db: MongoDatabase, clock: Clock)(implicit cs: ContextShift[IO])
  extends ChallengeStore[IO] {

  import Conversions._
  import Schema._

  val ChallengeCollection = "posts.challenges"
  val ChallengedCollection = "posts.challenges.challenged"
  val ReportCollection = "posts.challenges.reports"

  val challengeCollection = db.getCollection(ChallengeCollection)
  val challengedCollection = db.getCollection(ChallengedCollection)
  val reportCollection = db.getCollection(ReportCollection)


  override def register(challenge: Challenge): IO[Unit] = unitIO {
    challengeCollection
      .insertOne(challengeToDoc(challenge))
  }

  override def getById(challengeId: ChallengeId): IO[Option[Challenge]] = firstIO {
    challengeCollection
      .find(eql("challenge_id", challengeId.value.uuid))
      .limit(1)
      .map(docToChallenge(_).toOption)
  }

  override def getByAuthor(userId: UserId): IO[List[ChallengeId]] = toListIO {
    challengeCollection
      .find(eql("author", userId.value.uuid))
      .map(asChallengeId(_))
  }

  override def getByAuthor(author: UserId,
                           page: Page,
                           predicate: Challenge => Boolean): IO[List[ChallengeId]] = toPagedListIO(page) {
    challengeCollection
      .find(eql("author", author.value.uuid))
      .sort(descending("created"))
      .map(docToChallenge(_).toOption.filter(predicate).map(_.id))
      .filter(_.isDefined)
      .map(_.get)
  }

  override def getByUser(userId: UserId): IO[List[ChallengeId]] = for {
    asAuthor     <- getByAuthor(userId)
    asChallengee <- toListIO {
      challengedCollection
        .find(eql("challengee_id", userId.value.uuid))
        .sort(descending("created"))
        .map(asChallengeId(_))
    }
  } yield asChallengee ++ asAuthor

  override def getByUser(userId: UserId, page: Page, predicate: Challenge => Boolean): IO[List[ChallengeId]] = for {
    asAuthor     <- getByAuthor(userId)
    asChallengee <- toListIO {
      challengedCollection
        .find(eql("challengee_id", userId.value.uuid))
        .map(asChallengeId(_))
    }
    challengeIds = (asAuthor ++ asChallengee).distinct.map(_.value.uuid)
    filteredChallenges <- toPagedListIO(page) {
      challengeCollection
        .find(in("challenge_id", challengeIds:_*))
        .map(docToChallenge(_).toOption.filter(predicate).map(_.id))
        .filter(_.isDefined)
        .map(_.get)
    }
  } yield filteredChallenges

  override def getResultsByChallenge(challengeId: ChallengeId): IO[List[Either[Unit, List[ChallengeStepReportEntry]]]] = toListIO {
    reportCollection
      .find(eql("challenge_id", challengeId.value.uuid))
      .map { getList(_,"reports")
        .map(docToChallengeStepEntry(_))
        .foldLeft(Right(List()): Either[Unit, List[ChallengeStepReportEntry]]) {
          case (Left(_), _)               => Left(())
          case (_, Left(err))             => Left(())
          case (Right(acc), Right(entry)) => Right(entry:: acc)
        }
      }
  }

  override def getChallengees(challengeId: ChallengeId, page: Page): IO[List[UserId]] = toListIO {
    challengedCollection
      .find(eql("challenge_id", challengeId.value.uuid))
      .paged(page)
      .map(_.getString("challengee_id"))
      .map(UUID.unsafeFrom(_))
      .map(UserId(_))
  }

  override def getChallengeeCount(challengeId: ChallengeId): IO[Long] = firstIO {
    challengedCollection
      .countDocuments(eql("challenge_id", challengeId.value.uuid))
  }

  override def acceptChallenge(userId: UserId, challengeId: ChallengeId): IO[Unit] = for {
    _ <- unitIO {
      challengedCollection
        .updateOne(and(
          eql("challenge_id", challengeId.value.uuid),
          eql("challengee_id", userId.value.uuid)),
          combine(
            setOnInsert("challenge_id",  challengeId.value.uuid),
            setOnInsert("challengee_id", userId.value.uuid),
            setOnInsert("timestamp", clock.now().value),
            set("status", AcceptanceStatus.format(ChallengeAccepted))
          ),
          (new UpdateOptions).upsert(true))
     }
    // Creating an empty entry in the report collection when challenge is accepted
    _ <- unitIO {
      reportCollection
        .updateOne(and(
          eql("challenge_id", challengeId.value.uuid),
          eql("challengee_id", userId.value.uuid)),
        combine(
          setOnInsert("challenge_id",  challengeId.value.uuid),
          setOnInsert("challengee_id", userId.value.uuid),
          setOnInsert("reports", List())),
        (new UpdateOptions).upsert(true))
    }
  } yield ()

  override def isChallengeAccepted(challengeId: ChallengeId, byUser: UserId): IO[Boolean] = firstIO {
    challengedCollection
      .find(and(
        eql("challenge_id", challengeId.value.uuid),
        eql("challengee_id", byUser.value.uuid)))
      .map(_.getString("status"))
      .map(AcceptanceStatus.from(_) == ChallengeAccepted)
  }

  override def isChallengeRejected(challengeId: ChallengeId, byUser: UserId): IO[Boolean] = firstIO {
    challengedCollection
      .find(and(
        eql("challenge_id", challengeId.value.uuid),
        eql("challengee_id", byUser.value.uuid)))
      .map(_.getString("status"))
      .map(AcceptanceStatus.from(_) == ChallengeRejected)
  }

  override def rejectChallenge(userId: UserId, challengeId: ChallengeId): IO[Unit] = unitIO {
    challengedCollection
      .updateOne(and(
        eql("challenge_id", challengeId.value.uuid),
        eql("challengee_id", userId.value.uuid)),
        set("status", AcceptanceStatus.format(ChallengeRejected)))
  }

  override def cancelChallenge(userId: UserId, challengeId: ChallengeId): IO[Unit] = unitIO {
    challengedCollection
      .updateOne(and(
        eql("challenge_id", challengeId.value.uuid),
        eql("challengee_id", userId.value.uuid)),
        set("status", AcceptanceStatus.format(ChallengeCancelled)))
  }

  override def getStepReports(userId: UserId, challengeId: ChallengeId): IO[List[ChallengeStepReportEntry]] = firstOptionIO {
    reportCollection
      .find(and(
        eql("challenge_id", challengeId.value.uuid),
        eql("challengee_id", userId.value.uuid)))
      .limit(1)
      .map { getList(_,"reports")
        .flatMap(docToChallengeStepEntry(_).toOption)
        .toList
      }
  }.map(_.getOrElse(List()))

  override def reportStep(userId: UserId, challengeId: ChallengeId, step: Int, status: ChallengeStepReportStatus): IO[Unit] = for {
    reports    <- getStepReports(userId, challengeId)
    newReports <- IO(ChallengeStepReportEntry(step, status) :: reports.filterNot(_.step==step))
        .map(_.map(challengeStepEntryToDoc(_)))
    _          <- unitIO {
      reportCollection.updateOne(
        and(eql("challenge_id", challengeId.value.uuid),
            eql("challengee_id", userId.value.uuid)),
        Document("$set" -> Document("reports" -> newReports)) // set(...) doesn't work for arrays ...
      )
    }
  } yield ()

  override def markUserAsChallenged(userId: UserId, challengeId: ChallengeId): IO[Unit] = unitIO {
    challengedCollection
      .insertOne(challengeeToDoc(Challengee(challengeId, userId, clock.now(), ChallengeNotYetAccepted)))
  }

}
