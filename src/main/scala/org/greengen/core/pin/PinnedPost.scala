package org.greengen.core.pin

import org.greengen.core.UTCTimestamp
import org.greengen.core.post.PostId

case class PinnedPost(postId: PostId, timestamp: UTCTimestamp)
