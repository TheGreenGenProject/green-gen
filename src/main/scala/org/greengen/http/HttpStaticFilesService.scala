package org.greengen.http

import java.util.concurrent.Executors

import cats.data.NonEmptyList
import cats.effect._
import org.http4s.CacheDirective.`no-cache`
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.`Cache-Control`

import scala.concurrent.ExecutionContext

object HttpStaticFilesService {

  val BlockingContext: Blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4)))

  def nonAuthRoutes(implicit cs: ContextShift[IO]) = HttpRoutes.of[IO] {
    // GET
    case req @ GET -> Root =>
      StaticFile
        .fromResource(s"static/index.html", BlockingContext, Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .getOrElseF(NotFound())
    case req @ GET -> Root / filename =>
      StaticFile
        .fromResource(s"static/${filename}", BlockingContext, Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .getOrElseF(NotFound())
    case req @ GET -> Root / "assets" / filename =>
      StaticFile
        .fromResource(s"static/assets/${filename}", BlockingContext, Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .getOrElseF(NotFound())

  }

}
