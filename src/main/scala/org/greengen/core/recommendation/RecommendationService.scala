package org.greengen.core.recommendation

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


trait RecommendationService {

  def recommendPosts(user: UserId, n: Int): List[Recommendation[PostId]]

  def recommendUsers(user: UserId, n: Int): List[Recommendation[UserId]]

}
