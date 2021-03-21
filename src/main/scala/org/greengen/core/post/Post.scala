package org.greengen.core.post

import org.greengen.core.challenge.ChallengeId
import org.greengen.core.event.EventId
import org.greengen.core.poll.PollId
import org.greengen.core.tip.TipId
import org.greengen.core.user.UserId
import org.greengen.core.{Hashtag, Source, UTCTimestamp, UUID}


case class PostId(value: UUID)

object PostId {
  def newId = PostId(UUID.random())
}


sealed trait Post {
  val id: PostId
  val author: UserId
  val created: UTCTimestamp
  val hashtags: Set[Hashtag]
}

// Repost of someone else post
case class RePost(id: PostId,
                  author: UserId,
                  originalId: PostId,
                  created: UTCTimestamp,
                  hashtags: Set[Hashtag]) extends Post

// Organised event - with a schedule, participants, etc.
case class EventPost(id: PostId,
                     author: UserId,
                     event: EventId,
                     created: UTCTimestamp,
                     hashtags: Set[Hashtag]) extends Post

// Challenge - post a challenge for the user, open to anybody
case class ChallengePost(id: PostId,
                         author: UserId,
                         challenge: ChallengeId,
                         created: UTCTimestamp,
                         hashtags: Set[Hashtag]) extends Post

// Tip - Post a tip on a topic
case class TipPost(id: PostId,
                   author: UserId,
                   tip: TipId,
                   created: UTCTimestamp,
                   hashtags: Set[Hashtag]) extends Post

// Creating a poll where followers can answer
case class PollPost(id: PostId,
                    author: UserId,
                    poll: PollId,
                    created: UTCTimestamp,
                    hashtags: Set[Hashtag]) extends Post

// Normal wall post
case class FreeTextPost(id: PostId,
                        author: UserId,
                        content: String,
                        sources: List[Source],
                        created: UTCTimestamp,
                        hashtags: Set[Hashtag]) extends Post