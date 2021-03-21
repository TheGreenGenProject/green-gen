package org.greengen.core

import org.greengen.core.post.PostId

sealed trait Source
case object MySelf extends Source
case class FromPost(post: PostId) extends Source
case class AcademicReference(value: String) extends Source
case class Web(url: Url) extends Source