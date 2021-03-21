package org.greengen.main

import cats.effect.{ContextShift, IO, Timer}
import org.greengen.core.Clock
import org.greengen.http.auth.{HttpAuthService, TokenAuthMiddleware}
import org.greengen.http.user.HttpUserService
import org.greengen.impl.inmemory._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import cats.implicits._
import org.greengen.http.challenge.HttpChallengeService
import org.greengen.http.feed.HttpFeedService
import org.greengen.http.wall.HttpWallService
import org.greengen.http.follower.HttpFollowerService
import org.greengen.http.hashtag.HttpHashtagService
import org.greengen.http.like.HttpLikeService
import org.greengen.http.notification.HttpNotificationService
import org.greengen.http.pin.HttpPinService
import org.greengen.http.post.HttpPostService
import org.greengen.http.ranking.HttpRankingService
import org.greengen.http.tip.HttpTipService
import org.http4s.server.middleware.CORS

import scala.concurrent.ExecutionContext.Implicits.global


object InMemoryGreenGenServer extends App {

  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  // Services
  val clock = Clock()
  val userService = new InMemoryUserService(clock)
  val authService = new InMemoryAuthService(clock, userService)
  val notificationService = new InMemoryNotificationService(clock, userService)
  val followerService = new InMemoryFollowerService(clock, userService, notificationService)
  val tipService = new InMemoryTipService(clock, userService)
  val challengeService = new InMemoryChallengeService(clock, userService, followerService, notificationService)
  val hashtagService = new InMemoryHashtagService(userService)
  val feedService = new InMemoryFeedService(userService, followerService)
  val wallService = new InMemoryWallService(userService, feedService)
  val postService = new InMemoryPostService(clock, userService, wallService)
  val likeService = new InMemoryLikeService(clock, userService, notificationService, postService)
  val pinService = new InMemoryPinService(clock, userService, postService)
  val eventService = new InMemoryEventService(clock, userService, notificationService)
  val reminderService = new InMemoryReminderService(clock, eventService, notificationService)
  val rankingService = new InMemoryRankingService(userService, likeService, followerService, postService, eventService)

  // FIXME Populating data for testing purpose
  TestData
    .init(clock, authService,
      userService, followerService,
      tipService, challengeService,
      postService, eventService,
      notificationService)
    .unsafeRunSync()

  // Token based authentication middleware
  val authMiddleware = TokenAuthMiddleware.authMiddleware(authService)

  // Routes
  val nonAuthRoutes =
    HttpAuthService.nonAuthRoutes(authService) <+>
    HttpUserService.nonAuthRoutes(userService)
  val authRoutes =
    authMiddleware(HttpPostService.routes(clock, postService)) <+>
    authMiddleware(HttpFollowerService.routes(followerService)) <+>
    authMiddleware(HttpHashtagService.routes(hashtagService)) <+>
    authMiddleware(HttpLikeService.routes(likeService)) <+>
    authMiddleware(HttpWallService.routes(wallService)) <+>
    authMiddleware(HttpFeedService.routes(feedService)) <+>
    authMiddleware(HttpTipService.routes(tipService)) <+>
    authMiddleware(HttpChallengeService.routes(clock, challengeService)) <+>
    authMiddleware(HttpPinService.routes(pinService)) <+>
    authMiddleware(HttpNotificationService.routes(clock, notificationService)) <+>
    authMiddleware(HttpRankingService.routes(rankingService)) <+>
    authMiddleware(HttpAuthService.routes(authService)) <+>
    authMiddleware(HttpUserService.routes(userService))
  val routes = CORS(nonAuthRoutes <+> authRoutes) // CORS required for browsers

  // App
  val httpApp = Router("/" -> routes).orNotFound
  val serverBuilder = BlazeServerBuilder[IO](global)
    .bindHttp(8080, "localhost")
    .withHttpApp(httpApp)
  val fiber = serverBuilder.resource
    .use(_ => IO.never)
    .start
    .unsafeRunSync()

  Thread.sleep(3600 * 1000)
}
