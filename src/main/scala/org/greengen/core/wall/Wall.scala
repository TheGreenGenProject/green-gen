package org.greengen.core.wall

import org.greengen.core.post.PostId
import org.greengen.core.user.UserId


case class Wall(user: UserId, posts: List[PostId])

