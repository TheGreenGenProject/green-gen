package org.greengen.db.mongo

import org.greengen.core.{Hash, UTCTimestamp, UUID}
import org.greengen.core.user.{Profile, Pseudo, User, UserId}
import org.greengen.db.mongo.Conversions.{bytes2Hex, hexToBytes}
import org.mongodb.scala.bson.collection.Document


object Schema {

  def userIdToDocument(userId: UserId): Document =
    Document("user_id" -> userId.value.uuid)

  def docToUserId(doc: Document): Either[String, UserId] = for {
    str <- Option(doc.getString("user_id")).toRight(s"No field user_id found in $doc")
    uuid <- UUID.from(str).toRight(s"Invalid UUID $str")
  } yield UserId(uuid)

  def userProfileToDoc(user: User, profile: Profile): Document =
    Document(
      "user_id"       -> user.id.value.uuid,
      "credentials"   -> Document(
        "email_hash"    -> user.emailHash.toString,
        "password_hash" -> user.passwordHash.toString),
      "enabled"       -> user.enabled,
      "profile"       -> profileToDoc(profile))

  def docToUserProfile(doc: Document): Either[String, (User, Profile)] = for {
    uuid <- Option(doc.getString("user_id"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field user_id or invalid UUID found in $doc")
    credentials <- doc.get("credentials").map(_.asDocument)
      .toRight(s"No field credentials found in $doc")
    emailHash <- Option(credentials.get("email_hash").asString.getValue)
      .map(hexToBytes)
      .map(Hash(_))
      .toRight(s"No field credentials.email_hash found in $doc")
    passwordHash <- Option(credentials.get("password_hash").asString.getValue)
      .map(hexToBytes)
      .map(Hash(_))
      .toRight(s"No field credentials.password_hash found in $doc")
    enabled <- Option(doc.getBoolean("enabled"))
      .toRight(s"No field enabled found in $doc")
    profile <- docToProfile(doc)
  } yield (User(uuid, emailHash, passwordHash, enabled), profile)

  def profileToDoc(profile: Profile): Document =
    Document(
      "pseudo"   -> profile.pseudo.value,
      "intro"    -> profile.intro,
      "since"    -> profile.since.value,
      "verified" -> profile.verified
    )

  def docToProfile(doc: Document): Either[String, Profile] = for {
    uuid <- Option(doc.getString("user_id"))
      .flatMap(UUID.from)
      .map(UserId(_))
      .toRight(s"No field 'user_id' or invalid UUID found in $doc")
    profile <- doc.get("profile").map(_.asDocument)
        .toRight(s"No field 'profile' found in $doc")
    pseudo <- Option(profile.get("pseudo").asString.getValue)
      .map(Pseudo(_))
      .toRight(s"No field 'pseudo' found in $doc")
    intro <- Right(Option(profile.get("intro").asString.getValue))
    since <- Option(profile.get("since").asInt64.getValue)
      .map(UTCTimestamp(_))
      .toRight(s"No field 'since' found in $doc")
    verified <- Option(profile.get("verified").asBoolean.getValue)
      .toRight(s"No field 'verified' found in $doc")
  } yield Profile(uuid, pseudo, since, intro, verified)

}
