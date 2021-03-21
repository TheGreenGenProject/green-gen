package org.greengen.core.follower

import org.greengen.core.Hashtag
import org.greengen.core.challenge.ChallengeId
import org.greengen.core.conversation.ConversationId
import org.greengen.core.event.EventId
import org.greengen.core.user.UserId

sealed trait Follower
case class FollowUsers(user: UserId, ids: Set[UserId]) extends Follower
case class FollowConversations(user: UserId, ids: Set[ConversationId]) extends Follower
case class FollowChallenges(user: UserId, ids: Set[ChallengeId]) extends Follower
case class FollowEvents(user: UserId, ids: Set[EventId]) extends Follower
case class FollowHashtags(user: UserId, hashtags: Set[Hashtag]) extends Follower