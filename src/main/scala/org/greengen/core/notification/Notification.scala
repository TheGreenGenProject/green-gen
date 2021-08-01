package org.greengen.core.notification

import org.greengen.core.{Clock, UTCTimestamp, UUID}
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.poll.PollId
import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

case class NotificationId(id: UUID)

object NotificationId {
  def newId = NotificationId(UUID.random())
}


sealed trait NotificationContent
case class PlatformMessageNotification(message: String) extends NotificationContent
case class EventModifiedNotification(eventId: EventId) extends NotificationContent
case class EventCancelledNotification(eventId: EventId) extends NotificationContent
case class NewFollowerNotification(follower: UserId) extends NotificationContent
case class PostLikedNotification(postId: PostId, likedBy: UserId) extends NotificationContent
case class YouHaveBeenChallengedNotification(challengeId: ChallengeId) extends NotificationContent
case class ChallengeAcceptedNotification(challengeId: ChallengeId, userId: UserId) extends NotificationContent
case class ChallengeRejectedNotification(challengeId: ChallengeId, userId: UserId) extends NotificationContent
case class PollAnsweredNotification(pollId: PollId, userId: UserId) extends NotificationContent

case class Notification(
  id: NotificationId,
  content: NotificationContent,
  timestamp: UTCTimestamp)

object Notification {
  def from(clock: Clock, content: NotificationContent): Notification =
    Notification(NotificationId.newId, content, clock.now())
}

case class NotificationWithReadStatus(
  notification: Notification,
  status: ReadStatus)

sealed trait ReadStatus
case class Read(timestamp: UTCTimestamp) extends ReadStatus
case object Unread extends ReadStatus

