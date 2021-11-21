package org.greengen.main

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import org.greengen.core.Clock
import org.greengen.db.mongo
import org.greengen.http.HttpStaticFilesService
import org.greengen.http.auth.{HttpAuthService, TokenAuthMiddleware}
import org.greengen.http.challenge.HttpChallengeService
import org.greengen.http.conversation.HttpConversationService
import org.greengen.http.event.HttpEventService
import org.greengen.http.feed.HttpFeedService
import org.greengen.http.follower.HttpFollowerService
import org.greengen.http.hashtag.HttpHashtagService
import org.greengen.http.like.HttpLikeService
import org.greengen.http.notification.HttpNotificationService
import org.greengen.http.partnership.HttpPartnershipService
import org.greengen.http.pin.HttpPinService
import org.greengen.http.poll.HttpPollService
import org.greengen.http.post.{HttpAggregatedPostService, HttpPostService}
import org.greengen.http.ranking.HttpRankingService
import org.greengen.http.registration.HttpRegistrationService
import org.greengen.http.tip.HttpTipService
import org.greengen.http.user.HttpUserService
import org.greengen.http.wall.HttpWallService
import org.greengen.impl.auth.AuthServiceImpl
import org.greengen.impl.challenge.ChallengeServiceImpl
import org.greengen.impl.conversation.ConversationServiceImpl
import org.greengen.impl.event.EventServiceImpl
import org.greengen.impl.feed.FeedServiceImpl
import org.greengen.impl.follower.FollowerServiceImpl
import org.greengen.impl.hashtag.HashtagServiceImpl
import org.greengen.impl.like.LikeServiceImpl
import org.greengen.impl.notification.NotificationServiceImpl
import org.greengen.impl.partnership.PartnershipServiceImpl
import org.greengen.impl.pin.PinServiceImpl
import org.greengen.impl.poll.PollServiceImpl
import org.greengen.impl.post.PostServiceImpl
import org.greengen.impl.ranking.{CachedRankingService, RankingServiceImpl}
import org.greengen.impl.registration.RegistrationServiceImpl
import org.greengen.impl.reminder.ReminderServiceImpl
import org.greengen.impl.tip.TipServiceImpl
import org.greengen.impl.user.UserServiceImpl
import org.greengen.impl.wall.WallServiceImpl
import org.greengen.main.InMemoryGreenGenServer.contextShift
import org.greengen.store.auth.InMemoryAuthStore
import org.greengen.store.challenge.MongoChallengeStore
import org.greengen.store.conversation.{CachedConversationStore, MongoConversationStore}
import org.greengen.store.event.{InMemoryEventStore, MongoEventStore}
import org.greengen.store.feed.MongoFeedStore
import org.greengen.store.follower.{CachedFollowerStore, MongoFollowerStore}
import org.greengen.store.hashtag.MongoHashtagStore
import org.greengen.store.like.{CachedLikeStore, MongoLikeStore}
import org.greengen.store.notification.MongoNotificationStore
import org.greengen.store.partnership.InMemoryPartnershipStore
import org.greengen.store.pin.{CachedPinStore, MongoPinStore}
import org.greengen.store.poll.MongoPollStore
import org.greengen.store.post.{CachedPostStore, InMemoryPostStore, MongoPostStore}
import org.greengen.store.registration.InMemoryRegistrationStore
import org.greengen.store.tip.MongoTipStore
import org.greengen.store.user.{CachedUserStore, MongoUserStore}
import org.greengen.store.wall.MongoWallStore
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.CORS

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object GreenGenServer extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    val dbUrl = Option(System.getenv("MONGODB_URI")) match {
      case Some(url) => url
      case None      => throw new RuntimeException("Environment variable MONGODB_URI is not defined")
    }

    val db = mongo.Database.connection(dbUrl)

    // Services
    val clock = Clock()
    val userService = {
      val cachedStore = CachedUserStore.withCache(new MongoUserStore(db))
      new UserServiceImpl(cachedStore)(clock)
    }
    val authService = new AuthServiceImpl(new InMemoryAuthStore)(clock, userService)
    val registrationService = new RegistrationServiceImpl(new InMemoryRegistrationStore(clock))(clock, userService)
    val notificationService = new NotificationServiceImpl(new MongoNotificationStore(db, clock))(clock, userService)
    val followerService = {
      val cachedStore = CachedFollowerStore.withCache(new MongoFollowerStore(db, clock))
      new FollowerServiceImpl(cachedStore)(clock, userService, notificationService)
    }
    val tipService = new TipServiceImpl(new MongoTipStore(db))(clock, userService)
    val challengeService = new ChallengeServiceImpl(new MongoChallengeStore(db, clock))(clock, userService, followerService, notificationService)
    val pollService = new PollServiceImpl(new MongoPollStore(db, clock))(clock, userService)
    val hashtagService = new HashtagServiceImpl(new MongoHashtagStore(db, clock))(userService)
    val feedService = new FeedServiceImpl(new MongoFeedStore(db, clock))(userService, followerService, hashtagService)
    val wallService = new WallServiceImpl(new MongoWallStore(db, clock))(userService)
    val postService = {
      val cachedStore = CachedPostStore.withCache(new MongoPostStore(db))
      new PostServiceImpl(cachedStore)(clock, userService, wallService, feedService)
    }
    val likeService = {
      val cachedStore = CachedLikeStore.withCache(new MongoLikeStore(db, clock))
      new LikeServiceImpl(cachedStore)(clock, userService, notificationService, postService)
    }
    val pinService = {
      val cachedStore = CachedPinStore.withCache(new MongoPinStore(db))
      new PinServiceImpl(cachedStore)(clock, userService, postService)
    }
    val eventService = new EventServiceImpl(new MongoEventStore(db, clock))(clock, userService, notificationService)
    val reminderService = new ReminderServiceImpl(clock, eventService, notificationService)
    val conversationService = {
      val cachedStore = CachedConversationStore.withCache(new MongoConversationStore(db, clock))
      new ConversationServiceImpl(cachedStore)(clock, userService, notificationService)
    }
    val rankingService = {
      val service = new RankingServiceImpl(userService, likeService, followerService, postService, eventService)
      CachedRankingService.withCache(clock, retention = 15.minutes)(service)
    }
    val partnershipService = new PartnershipServiceImpl(new InMemoryPartnershipStore)(clock, userService)

    // Token based authentication middleware
    val authMiddleware = TokenAuthMiddleware.authMiddleware(authService)

    // Routes
    val nonAuthRoutes =
      HttpStaticFilesService.nonAuthRoutes(contextShift) <+>
      HttpAuthService.nonAuthRoutes(authService) <+>
        HttpRegistrationService.nonAuthRoutes(registrationService)
    val authRoutes =
      authMiddleware(HttpPostService.routes(clock, postService)) <+>
      authMiddleware(HttpAggregatedPostService.routes(clock: Clock,
          userService,
          postService,
          challengeService,
          pollService,
          eventService,
          tipService,
          pinService,
          likeService,
          followerService,
          partnershipService,
          conversationService)) <+>
        authMiddleware(HttpFollowerService.routes(followerService)) <+>
        authMiddleware(HttpHashtagService.routes(hashtagService)) <+>
        authMiddleware(HttpLikeService.routes(likeService)) <+>
        authMiddleware(HttpWallService.routes(wallService)) <+>
        authMiddleware(HttpFeedService.routes(feedService)) <+>
        authMiddleware(HttpTipService.routes(tipService)) <+>
        authMiddleware(HttpPollService.routes(clock, pollService)) <+>
        authMiddleware(HttpChallengeService.routes(clock, challengeService)) <+>
        authMiddleware(HttpEventService.routes(clock, eventService)) <+>
        authMiddleware(HttpPinService.routes(pinService)) <+>
        authMiddleware(HttpNotificationService.routes(clock, notificationService)) <+>
        authMiddleware(HttpConversationService.routes(clock, conversationService)) <+>
        authMiddleware(HttpRankingService.routes(rankingService)) <+>
        authMiddleware(HttpPartnershipService.routes(partnershipService)) <+>
        authMiddleware(HttpAuthService.routes(authService)) <+>
        authMiddleware(HttpUserService.routes(userService))
    val routes = CORS(nonAuthRoutes <+> authRoutes) // CORS required for browsers

    // App
    val port = Option(System.getenv("PORT"))
      .map(_.toInt)
      .getOrElse(8080)
    println(s"App will be opened on port $port")
    val httpApp = Router("/" -> routes).orNotFound
    BlazeServerBuilder[IO](global)
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
