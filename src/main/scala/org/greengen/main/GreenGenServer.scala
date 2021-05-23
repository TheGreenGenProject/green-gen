package org.greengen.main

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import org.greengen.core.Clock
import org.greengen.db.mongo
import org.greengen.http.auth.{HttpAuthService, TokenAuthMiddleware}
import org.greengen.http.challenge.HttpChallengeService
import org.greengen.http.feed.HttpFeedService
import org.greengen.http.follower.HttpFollowerService
import org.greengen.http.hashtag.HttpHashtagService
import org.greengen.http.like.HttpLikeService
import org.greengen.http.notification.HttpNotificationService
import org.greengen.http.pin.HttpPinService
import org.greengen.http.post.HttpPostService
import org.greengen.http.ranking.HttpRankingService
import org.greengen.http.tip.HttpTipService
import org.greengen.http.user.HttpUserService
import org.greengen.http.wall.HttpWallService
import org.greengen.impl.auth.AuthServiceImpl
import org.greengen.impl.challenge.ChallengeServiceImpl
import org.greengen.impl.event.EventServiceImpl
import org.greengen.impl.feed.FeedServiceImpl
import org.greengen.impl.follower.FollowerServiceImpl
import org.greengen.impl.hashtag.HashtagServiceImpl
import org.greengen.impl.like.LikeServiceImpl
import org.greengen.impl.notification.NotificationServiceImpl
import org.greengen.impl.pin.PinServiceImpl
import org.greengen.impl.post.PostServiceImpl
import org.greengen.impl.ranking.RankingServiceImpl
import org.greengen.impl.reminder.ReminderServiceImpl
import org.greengen.impl.tip.TipServiceImpl
import org.greengen.impl.user.UserServiceImpl
import org.greengen.impl.wall.WallServiceImpl
import org.greengen.store.auth.InMemoryAuthStore
import org.greengen.store.challenge.{InMemoryChallengeStore, MongoChallengeStore}
import org.greengen.store.event.InMemoryEventStore
import org.greengen.store.feed.InMemoryFeedStore
import org.greengen.store.follower.InMemoryFollowerStore
import org.greengen.store.hashtag.InMemoryHashtagStore
import org.greengen.store.like.{InMemoryLikeStore, MongoLikeStore}
import org.greengen.store.notification.InMemoryNotificationStore
import org.greengen.store.pin.InMemoryPinStore
import org.greengen.store.post.{InMemoryPostStore, MongoPostStore}
import org.greengen.store.tip.{InMemoryTipStore, MongoTipStore}
import org.greengen.store.user.{InMemoryUserStore, MongoUserStore}
import org.greengen.store.wall.InMemoryWallStore
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.CORS

import scala.concurrent.ExecutionContext.Implicits.global


object GreenGenServer extends App {

  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val db = mongo.Database.connection(
    "mongodb://localhost:27017/",
    "greengen",
    "gogogo")

  // Services
  val clock = Clock()
  val userService = new UserServiceImpl(new MongoUserStore(db))(clock)
  val authService = new AuthServiceImpl(new InMemoryAuthStore)(clock, userService)
  val notificationService = new NotificationServiceImpl(new InMemoryNotificationStore)(clock, userService)
  val followerService = new FollowerServiceImpl(new InMemoryFollowerStore)(clock, userService, notificationService)
  val tipService = new TipServiceImpl(new MongoTipStore(db))(clock, userService)
  val challengeService = new ChallengeServiceImpl(new MongoChallengeStore(db, clock))(clock, userService, followerService, notificationService)
  val hashtagService = new HashtagServiceImpl(new InMemoryHashtagStore)(userService)
  val feedService = new FeedServiceImpl(new InMemoryFeedStore)(userService, followerService, hashtagService)
  val wallService = new WallServiceImpl(new InMemoryWallStore)(userService)
  val postService = new PostServiceImpl(new MongoPostStore(db))(clock, userService, wallService, feedService)
  val likeService = new LikeServiceImpl(new MongoLikeStore(db, clock))(clock, userService, notificationService, postService)
  val pinService = new PinServiceImpl(new InMemoryPinStore)(clock, userService, postService)
  val eventService = new EventServiceImpl(new InMemoryEventStore)(clock, userService, notificationService)
  val reminderService = new ReminderServiceImpl(clock, eventService, notificationService)
  val rankingService = new RankingServiceImpl(userService, likeService, followerService, postService, eventService)

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
