package org.greengen.store.user

import cats.effect.{ContextShift, IO}
import com.mongodb.client.model.Filters.{and, regex, eq => eql}
import com.mongodb.client.model.Updates.{set}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}
import org.greengen.core.{Hash, UUID}
import org.greengen.db.mongo.{Conversions, Schema}
import org.mongodb.scala.MongoDatabase


class MongoUserStore(db: MongoDatabase)
                    (implicit cs: ContextShift[IO]) extends UserStore[IO] {

  import Schema._
  import Conversions._

  val UsersCollection = "users"

  private[this] val usersCollection = db.getCollection(UsersCollection)

  override def register(userId: UserId,
                        userHash: Hash,
                        pwHash: Hash,
                        user: User,
                        profile: Profile): IO[Unit] = unitIO {
    usersCollection.insertOne(userProfileToDoc(user, profile))
  }

  override def updateProfile(userId: UserId, profile: Profile): IO[Unit] = unitIO {
    usersCollection.findOneAndUpdate(
      eql("user_id", profile.id.value.uuid),
      set("profile", profileToDoc(profile)))
  }

  override def setUserEnabled(userId: UserId, enabled: Boolean): IO[Unit] = unitIO {
    usersCollection.updateOne(
      eql("user_id", userId.value.uuid),
      set("enabled", enabled))
  }

  override def getByUserId(userId: UserId): IO[Option[(User, Profile)]] = firstIO {
    usersCollection
        .find(eql("user_id", userId.value.uuid))
        .first()
        .map(docToUserProfile)
        .map(_.toOption)
  }

  override def emailExists(emailHash: Hash): IO[Boolean] =  firstOptionIO {
    usersCollection
      .find(eql("credentials.email_hash",    emailHash.toString))
      .first()
  }.map(_.isDefined)

  override def getByHashes(hashes: (Hash, Hash)): IO[Option[UserId]] = firstOptionIO {
    val (emailHash, pwHash) = hashes
    usersCollection
      .find(and(
        eql("credentials.email_hash",    emailHash.toString),
        eql("credentials.password_hash", pwHash.toString)
      ))
      .first()
      .map(_.getString("user_id"))
      .map(UUID.unsafeFrom(_))
      .map(UserId(_))
  }

  override def getByPseudo(pseudo: Pseudo): IO[Option[UserId]] = firstOptionIO {
    usersCollection
      .find(eql("profile.pseudo", pseudo.value))
      .first()
      .map(_.getString("user_id"))
      .map(UUID.unsafeFrom(_))
      .map(UserId(_))
  }

  override def getByPseudoPrefix(prefix: String): IO[List[UserId]] = toListIO {
    usersCollection
      .find(regex("profile.pseudo", prefix + ".*"))
      .limit(250) // FIXME
      .map(_.getString("user_id"))
      .map(UUID.unsafeFrom(_))
      .map(UserId(_))
  }

  override def pseudoExists(pseudo: Pseudo): IO[Boolean] = firstOptionIO {
    usersCollection
      .find(eql("profile.pseudo", pseudo.value))
      .limit(1)
  }.map(_.isDefined)

  override def checkUser(id: UserId): IO[Unit] = for {
    docOpt <- firstOptionIO {
        usersCollection
          .find(eql("user_id", id.value.uuid))
          .limit(1)
      }
    _ <- IO.raiseWhen(docOpt.isEmpty)(new RuntimeException(s"Unknown user id $id"))
  } yield ()

  override def deleteUser(userId: UserId): IO[Option[(User, Profile)]] = firstOptionIO {
    for {
      res <- usersCollection
        .findOneAndDelete(eql("user_id", userId.value.uuid))
        .map(docToUserProfile)
    } yield res.toOption
  }.map(_.flatten)

  override def allUserIds(): IO[List[UserId]] = toListIO {
    usersCollection
      .distinct[String]("user_id")
      .map(id => UserId(UUID.unsafeFrom(id)))
  }

  override def activeUsers(): IO[List[UserId]] = toListIO {
    usersCollection
      .find(eql("enabled", true))
      .map(_.getString("user_id"))
      .map(id => UserId(UUID.unsafeFrom(id)))
  }

}
