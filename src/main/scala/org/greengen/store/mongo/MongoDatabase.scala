package org.greengen.store.mongo

import com.mongodb.client.model.Filters.{eq => eql}
import org.greengen.core.user.{User, UserId}
import org.greengen.core.{Hash, UUID, user}
import org.greengen.store.UserStore
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.Document
import org.mongodb.scala.model.Sorts._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object MongoDatabase {

  val UserCollection = "users"
  val EventCollection = "events"
  val ChallengeCollection = "challenges"
  val TipCollection = "events"
  val PollCollection = "polls"
  val WallCollection = "walls"
  val ConversationCollection = "conversations"

  def create(db: String, client: MongoClient): UserStore = {
    val users = client.getDatabase(db).getCollection(UserCollection)

    new UserStore {

      def byId(id: UUID)(implicit ec: ExecutionContext): Future[Iterable[User]] = {
        users
          .find(eql("uuid", id))
          .sort(orderBy(descending("timestamp")))
          .toFuture()
          .transform {
            case Success(user) =>
              Success(user.map(toUser))
            case Failure(ex) => Failure(ex)
          }
      }

      def add(user: User)(implicit ec: ExecutionContext): Future[User] = ???
    }
  }

  def toUser(d: Document) = {
    val uuid = UUID(d.get("uuid").get.asString.getValue)
    val emailHash = Hash.safeFrom(d.get("emailHash").get.asBinary.getData)
    val pwHash = Hash.safeFrom(d.get("passwordHash").get.asBinary.getData)
    val enabled = d.get("enabled").get.asBoolean.getValue

    user.User(UserId(uuid), emailHash, pwHash, enabled)
  }
}
