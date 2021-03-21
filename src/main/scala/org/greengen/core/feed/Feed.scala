package org.greengen.core.feed

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId

case class Feed(userId: UserId, posts: List[PostId])
