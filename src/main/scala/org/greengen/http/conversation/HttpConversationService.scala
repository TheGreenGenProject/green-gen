package org.greengen.http.conversation

import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.greengen.core.{Clock, Page, UUID}
import org.greengen.core.conversation.{ConversationId, ConversationService, Message, MessageId}
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.greengen.http.HttpQueryParameters._


object HttpConversationService {

  val PageSize = 10

  def routes(clock: Clock, service: ConversationService[IO]) = AuthedRoutes.of[UserId, IO] {
    // GET
    case GET -> Root / "conversation" / "for-post" / UUIDVar(id) / IntVar(page) as _ =>
      for {
        conversationId <- service.getConversation(PostId(UUID.from(id)))
        messageIds     <- service.getConversationMessages(conversationId, Page(page, PageSize))
        messages       <- messageIds.map(service.getMessage(_)).sequence.map(_.flatten)
        res            <- Ok(messages.asJson)
      } yield res
    case GET -> Root / "conversation" / "messages" / "count" / UUIDVar(id) as _ =>
      service.countMessages(PostId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "conversation" / "messages" / UUIDVar(id) / IntVar(page) as _ =>
      service.getConversationMessages(ConversationId(UUID.from(id)), Page(page, PageSize)).flatMap(r => Ok(r.asJson))
    case GET -> Root / "conversation" / "message" / UUIDVar(id) as _ =>
      service.getMessage(MessageId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "conversation" / "message" / "is-flagged" / UUIDVar(id) as _ =>
      service.isFlagged(MessageId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case GET -> Root / "conversation" / "has" / "user" / "flagged" / UUIDVar(id) as userId =>
      service.hasUserFlagged(userId, MessageId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    // POST
    case POST -> Root / "conversation" / "message" :?
      PostIdQueryParamMatcher(postId) +&
      ContentQueryParamMatcher(content) as userId =>
      service.addMessage(postId, Message(MessageId.newId, userId, content, clock.now())).flatMap(r => Ok(r.asJson))
    case POST -> Root / "conversation" / "flag" / UUIDVar(id) as userId =>
      service.flag(userId, MessageId(UUID.from(id))).flatMap(r => Ok(r.asJson))
    case POST -> Root / "conversation" / "unflag" / UUIDVar(id) as userId =>
      service.unflag(userId, MessageId(UUID.from(id))).flatMap(r => Ok(r.asJson))
  }

}
