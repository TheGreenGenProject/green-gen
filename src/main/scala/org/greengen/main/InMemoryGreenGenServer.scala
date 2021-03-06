package org.greengen.main

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import org.greengen.core.Clock
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
import org.greengen.impl.ranking.RankingServiceImpl
import org.greengen.impl.registration.RegistrationServiceImpl
import org.greengen.impl.reminder.ReminderServiceImpl
import org.greengen.impl.tip.TipServiceImpl
import org.greengen.impl.user.UserServiceImpl
import org.greengen.impl.wall.WallServiceImpl
import org.greengen.store.auth.InMemoryAuthStore
import org.greengen.store.challenge.InMemoryChallengeStore
import org.greengen.store.conversation.InMemoryConversationStore
import org.greengen.store.event.InMemoryEventStore
import org.greengen.store.feed.InMemoryFeedStore
import org.greengen.store.follower.InMemoryFollowerStore
import org.greengen.store.hashtag.InMemoryHashtagStore
import org.greengen.store.like.InMemoryLikeStore
import org.greengen.store.notification.InMemoryNotificationStore
import org.greengen.store.partnership.InMemoryPartnershipStore
import org.greengen.store.pin.InMemoryPinStore
import org.greengen.store.poll.InMemoryPollStore
import org.greengen.store.post.InMemoryPostStore
import org.greengen.store.registration.InMemoryRegistrationStore
import org.greengen.store.tip.InMemoryTipStore
import org.greengen.store.user.InMemoryUserStore
import org.greengen.store.wall.InMemoryWallStore
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.CORS

import scala.concurrent.ExecutionContext.Implicits.global


object InMemoryGreenGenServer extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    // Services
    val clock = Clock()
    val userService = new UserServiceImpl(new InMemoryUserStore)(clock)
    val authService = new AuthServiceImpl(new InMemoryAuthStore)(clock, userService)
    val registrationService = new RegistrationServiceImpl(new InMemoryRegistrationStore(clock))(clock, userService)
    val notificationService = new NotificationServiceImpl(new InMemoryNotificationStore(clock))(clock, userService)
    val followerService = new FollowerServiceImpl(new InMemoryFollowerStore)(clock, userService, notificationService)
    val tipService = new TipServiceImpl(new InMemoryTipStore)(clock, userService)
    val challengeService = new ChallengeServiceImpl(new InMemoryChallengeStore)(clock, userService, followerService, notificationService)
    val pollService = new PollServiceImpl(new InMemoryPollStore(clock))(clock, userService)
    val hashtagService = new HashtagServiceImpl(new InMemoryHashtagStore)(userService)
    val feedService = new FeedServiceImpl(new InMemoryFeedStore)(userService, followerService, hashtagService)
    val wallService = new WallServiceImpl(new InMemoryWallStore)(userService)
    val postService = new PostServiceImpl(new InMemoryPostStore)(clock, userService, wallService, feedService)
    val likeService = new LikeServiceImpl(new InMemoryLikeStore)(clock, userService, notificationService, postService)
    val pinService = new PinServiceImpl(new InMemoryPinStore)(clock, userService, postService)
    val eventService = new EventServiceImpl(new InMemoryEventStore(clock))(clock, userService, notificationService)
    val reminderService = new ReminderServiceImpl(clock, eventService, notificationService)
    val conversationService = new ConversationServiceImpl(new InMemoryConversationStore)(clock, userService, notificationService)
    val rankingService = new RankingServiceImpl(userService, likeService, followerService, postService, eventService)
    val partnershipService = new PartnershipServiceImpl(new InMemoryPartnershipStore)(clock,userService)

    // FIXME Populating data for testing purpose
    TestData
      .init(clock, authService,
        userService, followerService,
        tipService, challengeService, pollService,
        postService, eventService,
        notificationService, partnershipService)
      .unsafeRunSync()

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
