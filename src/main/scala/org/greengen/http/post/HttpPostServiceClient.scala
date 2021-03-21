package org.greengen.http.post


import cats.effect.IO
import org.greengen.core.post.{Post, PostId, PostService}
import org.greengen.core.user.UserId
import org.greengen.core.{Hashtag, Reason}
import org.greengen.http.HttpHelper.{post => httppost}
import org.greengen.http.JsonDecoder._
import org.http4s.Uri
import org.http4s.client._

class HttpPostServiceClient(httpClient: Client[IO], root: Uri) extends PostService[IO] {

  override def post(post: Post): IO[PostId] =
    throw new RuntimeException("Cannot create a new Post through HttpPostServiceClient") // FIXME not sure if this call makes sense !

  override def repost(user: UserId, post: PostId): IO[PostId] =
    httpClient.expect[PostId](httppost(root / "post" / "repost",
      "user-id" -> user,
      "post-id" -> post))

  override def flag(flaggedBy: UserId, post: PostId, reason: Reason): IO[Unit] =
    httpClient.expect[Unit](httppost(root / "post" / "flag",
      "post-id" -> post,
      "reason" -> reason.toString))

  override def isFlagged(post: PostId): IO[Boolean] =
    httpClient.expect[Boolean](root / "post" / "is-flagged" / post.value.uuid)

  override def byId(post: PostId): IO[Option[Post]] =
    httpClient.expect[Option[Post]](root / "post" / "by-id" / post.value.uuid)

  override def byAuthor(userId: UserId): IO[Set[PostId]] =
    httpClient.expect[Set[PostId]](root / "post" / "by-author" / userId.value.uuid)

  override def byHashtags(hashtags: Set[Hashtag]): IO[Set[PostId]] =
    httpClient.expect[Set[PostId]](root / "post" / "by-hashtags" / hashtags.mkString("+"))

  override def trendByHashtags(n: Int): IO[List[(Int, Hashtag)]] =
    httpClient.expect[List[(Int, Hashtag)]](root / "hashtag" / "trend" / "by-posts" / n.toString)
}
