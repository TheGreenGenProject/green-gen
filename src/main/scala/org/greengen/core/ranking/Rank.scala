package org.greengen.core.ranking

import org.greengen.core.user.Profile

case class Score(value: Long)
case class ScoreBreakdown(fromLikes: Long, fromFollows: Long, fromPosts: Long, fromEvents: Long) {
  def asScore = Score(fromFollows + fromPosts + fromEvents)
}

object ScoreBreakdown {

  def compute(profile: Profile,
              likeReceived: Long,
              followingCount: Long,
              followersCount: Long,
              postCount: Long,
              eventOrganized: Long,
              eventParticipation: Long): ScoreBreakdown = {
    // TODO Implement some magic formula here
    val likeScore = likeReceived
    val followScore = followingCount + (followersCount * 3)
    val postScore = postCount * 2
    val eventScore = eventParticipation * 5 + eventOrganized * 50
    ScoreBreakdown(fromLikes = likeScore, fromFollows = followScore, fromPosts = postScore, fromEvents = eventScore)
  }
}

// Ranking
sealed trait Rank {
  def maxScore: Score
}
case object GreenWood  extends Rank { val maxScore = Score(10) }
case object GaiaFriend extends Rank { val maxScore = Score(100) }
case object Converted  extends Rank { val maxScore = Score(1000) }
case object Influencer extends Rank { val maxScore = Score(2_500) }
case object Evangelist extends Rank { val maxScore = Score(10_000) }
case object Expert     extends Rank { val maxScore = Score(25_000) }
case object BlackBelt  extends Rank { val maxScore = Score(50_000) }
case object Sensei     extends Rank { val maxScore = Score(100_000) }
case object Shihan     extends Rank { val maxScore = Score(250_000) }
case object Hanshi     extends Rank { val maxScore = Score(500_000) }
case object OSensei    extends Rank { val maxScore = Score(750_000) }
case object Guru       extends Rank { val maxScore = Score(1_000_000) }


object Rank {

  def fromScore(score: Score): Rank = {
    if(score.value < GreenWood.maxScore.value) GreenWood
    else if(score.value < GaiaFriend.maxScore.value) GaiaFriend
    else if(score.value < Converted.maxScore.value) Converted
    else if(score.value < Influencer.maxScore.value) Influencer
    else if(score.value < Evangelist.maxScore.value) Evangelist
    else if(score.value < Expert.maxScore.value) Expert
    else if(score.value < BlackBelt.maxScore.value) BlackBelt
    else if(score.value < Sensei.maxScore.value) Sensei
    else if(score.value < Shihan.maxScore.value) Shihan
    else if(score.value < Hanshi.maxScore.value) Hanshi
    else if(score.value < OSensei.maxScore.value) OSensei
    else Guru
  }

}


