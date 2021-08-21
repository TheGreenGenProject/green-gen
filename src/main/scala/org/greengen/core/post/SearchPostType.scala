package org.greengen.core.post

trait SearchPostType
case object AllPosts extends SearchPostType
case object TipPosts extends SearchPostType
case object ChallengePosts extends SearchPostType
case object PollPosts extends SearchPostType
case object EventPosts extends SearchPostType
case object FreeTextPosts extends SearchPostType


object SearchPostType {

  // Reading a post type from a string
  def fromString(str: String): Option[SearchPostType] = Option(str).collect {
    case "all" => AllPosts
    case "tips" => TipPosts
    case "polls" => PollPosts
    case "challenges" => ChallengePosts
    case "events" => EventPosts
    case "free-texts" => FreeTextPosts
  }

}