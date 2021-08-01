package org.greengen.main

import cats.effect.IO
import cats.implicits._
import org.greengen.core.Coordinate.{LatLong, Latitude, Longitude}
import org.greengen.core._
import org.greengen.core.auth.AuthService
import org.greengen.core.challenge.{ChallengeId, ChallengeService, SuccessMeasure}
import org.greengen.core.event.{EventId, EventService}
import org.greengen.core.follower.FollowerService
import org.greengen.core.notification.NotificationService
import org.greengen.core.poll.{PollId, PollOption, PollService}
import org.greengen.core.post._
import org.greengen.core.tip.{TipId, TipService}
import org.greengen.core.user.{Pseudo, UserId, UserService}



object TestData {

  // Init test data
  def init(clock: Clock,
           authService: AuthService[IO],
           userService: UserService[IO],
           followerService: FollowerService[IO],
           tipService: TipService[IO],
           challengeService: ChallengeService[IO],
           pollService: PollService[IO],
           postService: PostService[IO],
           eventService: EventService[IO],
           notificationService: NotificationService[IO]): IO[Unit] = for {
    _ <- createUsers(authService, userService)
    _ <- makeFollowers(userService, followerService)
    _ <- createTips(userService, tipService)
    _ <- createPolls(userService, pollService)
    _ <- createChallenges(userService, challengeService)
    _ <- createPosts(userService, tipService, pollService, challengeService, postService)
    _ <- createEvents(userService, eventService)
    _ <- createNotifications(userService, notificationService)
  } yield ()

  private[this] def createUsers(authService: AuthService[IO], userService: UserService[IO]): IO[Unit] = for {
    _ <- chris(authService, userService)
    _ <- elisa(authService, userService)
    _ <- theblackcat(authService, userService)
    _ <- docsquirrel(authService, userService)
  } yield ()

  private[this] def makeFollowers(userService: UserService[IO], followerService :FollowerService[IO]): IO[Unit] = for {
    chrisId  <- userIdByPseudo(userService, "Chris")
    elisaId  <- userIdByPseudo(userService, "Elisa")
    thecatId <- userIdByPseudo(userService, "TheCat")
    _        <- followerService.startFollowing(chrisId, elisaId)
    _        <- followerService.startFollowing(elisaId, chrisId)
    _        <- followerService.startFollowing(elisaId, thecatId)
    _        <- followerService.startFollowing(thecatId, chrisId)
  } yield ()

  private[this] def createPosts(userService: UserService[IO],
                                tipService: TipService[IO],
                                pollService: PollService[IO],
                                challengeService: ChallengeService[IO],
                                postService: PostService[IO]): IO[Unit] = for {
    chrisId       <- userIdByPseudo(userService, "Chris")
    elisaId       <- userIdByPseudo(userService, "Elisa")
    docsquirrelId <- userIdByPseudo(userService, "DocSquirrel")
    catId         <- userIdByPseudo(userService, "TheCat")
    // FreeText
    freeTextId1   <- makeFreeTextPost(postService, chrisId)
    freeTextId2   <- makeFreeTextPost(postService, chrisId)
    _             <- makeFreeTextPost(postService, elisaId)
    _             <- makeFreeTextPost(postService, elisaId)
    _             <- makeFreeTextPost(postService, elisaId)
    _             <- postTipsFromAuthor(postService, tipService, elisaId)
    _             <- makeCountedFreeTextPost(postService, elisaId, 100)
    // Repost
    _             <- makeRepost(postService, elisaId, freeTextId1)
    _             <- makeRepost(postService, docsquirrelId, freeTextId2)
    // Tips
    _             <- postTipsFromAuthor(postService, tipService, elisaId)
    _             <- postTipsFromAuthor(postService, tipService, chrisId)
    _             <- postTipsFromAuthor(postService, tipService, catId)
    _             <- postTipsFromAuthor(postService, tipService, docsquirrelId)
    // Polls
    _             <- postPollsFromAuthor(postService, pollService, elisaId)
    _             <- postPollsFromAuthor(postService, pollService, chrisId)
    _             <- postPollsFromAuthor(postService, pollService, catId)
    _             <- postPollsFromAuthor(postService, pollService, docsquirrelId)
    // Challenges
    _             <- postChallengesFromAuthor(postService, challengeService, elisaId)
    _             <- postChallengesFromAuthor(postService, challengeService, chrisId)
    _             <- postChallengesFromAuthor(postService, challengeService, catId)
  } yield ()

  private[this] def createEvents(userService: UserService[IO],
                                 eventService: EventService[IO]): IO[Unit] = for {
    ownerId <- userIdByPseudo(userService, "Elisa")
    // FIXME this one doesn't seem to work ...
//    _       <- makeOnlineEvent(userService, eventService, ownerId)
    _       <- makeGeoLocatedEvent(userService, eventService, ownerId)
    _       <- makePhysicalEvent(userService, eventService, ownerId)
  } yield ()


  // Event Helper

  private[this] def makeOnlineEvent(userService: UserService[IO], eventService: EventService[IO], ownerId: UserId): IO[EventId] =
    makeEvent(userService, eventService, ownerId, OnLine(Url("www.green-gen.org")))

  private[this] def makeGeoLocatedEvent(userService: UserService[IO], eventService: EventService[IO], ownerId: UserId): IO[EventId] =
    makeEvent(userService, eventService, ownerId, GeoLocation(LatLong(Latitude(5.023), Longitude(67.45))))

  private[this] def makePhysicalEvent(userService: UserService[IO], eventService: EventService[IO], ownerId: UserId): IO[EventId] =
    makeEvent(userService, eventService, ownerId, Address(Some("somewhere"), Some("E2"), Country.UnitedKingdom))

  private[this] def makeEvent(userService: UserService[IO], eventService: EventService[IO], ownerId: UserId, location: Location): IO[EventId] = for {
    event   <- eventService.create(ownerId, 5,
      "This is a fake event happening somewhere on earth, organized by a cat - but nowhere near to your place.",
      location,
      Recurring(now(), Duration.OneHour, Duration.OneWeek, nextYear()))
    elisaId <- userIdByPseudo(userService, "Elisa")
    _       <- eventService.requestParticipation(elisaId, event.id)
    _       <- eventService.acceptParticipation(ownerId, elisaId, event.id)
    chrisId <- userIdByPseudo(userService, "Chris")
    _       <- eventService.requestParticipation(chrisId, event.id)
  } yield event.id


  // Post Helpers

  private[this] def makeCountedFreeTextPost(postService: PostService[IO], userId: UserId, count: Int): IO[Unit] = for {
    seq <- IO((1 to count).toList)
    _   <- seq.map { n => postService.post(FreeTextPost(
      PostId.newId,
      userId,
      s"$n - This is a counted free-text #ecology #green post.",
      List(myself),
      now(),
      ht("ecology", "green", "environment","env","noplanetb","plant-based","vegan-metal")))
    }.sequence
  } yield ()


  private[this] def makeFreeTextPost(postService: PostService[IO], userId: UserId): IO[PostId] =
    postService.post(FreeTextPost(
      PostId.newId,
      userId,
      "This is a free-text post about #ecology, #green, #env, #environment, #plantbased, #veganmetal, #noplanetb",
      List(myself),
      now(),
      ht("ecology", "green", "environment","env","noplanetb","plant-based","vegan-metal")))

  private[this] def makeRepost(postService: PostService[IO], userId: UserId, originalPostId: PostId): IO[PostId] =
    postService.post(RePost(
      PostId.newId,
      userId,
      originalPostId,
      now(),
      ht("ecologia", "verde", "something-else", "repost","green-stuff","green-beans")))

  private[this] def makeTipPost(postService: PostService[IO], userId: UserId, tipId: TipId): IO[PostId] =
    postService.post(TipPost(
      PostId.newId,
      userId,
      tipId,
      now(),
      ht("verde", "something-else", "tip")))

  private[this] def makePollPost(postService: PostService[IO], userId: UserId, pollId: PollId): IO[PostId] =
    postService.post(PollPost(
      PostId.newId,
      userId,
      pollId,
      now(),
      ht("survey", "question","randomquestionoftheday", "poll")))

  private[this] def postTipsFromAuthor(postService: PostService[IO],
                                       tipService: TipService[IO],
                                       author: UserId): IO[Unit] = for {
    tipIds <- tipService.byAuthor(author)
    _      <- tipIds.toList.map(makeTipPost(postService, author, _)).sequence
  } yield ()

  private[this] def postPollsFromAuthor(postService: PostService[IO],
                                        pollService: PollService[IO],
                                        author: UserId): IO[Unit] = for {
    pollIds <- pollService.byAuthor(author, Page.All)
    _       <- pollIds.map(makePollPost(postService, author, _)).sequence
  } yield ()

  private[this] def makeChallengePost(postService: PostService[IO],
                                      challengeService: ChallengeService[IO],
                                      userId: UserId,
                                      challengeId: ChallengeId): IO[PostId] = for {
    postId <- postService.post(ChallengePost(
      PostId.newId,
      userId,
      challengeId,
      now(),
      ht("verdura", "ichallengeyou","friendsbattle","no-plastic")))
    _ <- challengeService.challengeFollowers(userId, challengeId)
  } yield postId


  private[this] def postChallengesFromAuthor(postService: PostService[IO],
                                             challengeService: ChallengeService[IO],
                                             author: UserId): IO[Unit] = for {
    challengeIds <- challengeService.byAuthor(author, Page.All)
    _            <- challengeIds.map(makeChallengePost(postService, challengeService, author, _)).sequence
  } yield ()

  // User helpers

  private[this] def chris(authService: AuthService[IO], userService: UserService[IO]): IO[Unit] = for {
    emailHash <- IO.fromOption(Hash.md5("chris@green-gen.org"))(fail("Couldn't hash email"))
    pwHash    <- IO.fromOption(Hash.md5("gogogo"))(fail("Couldn't hash password"))
    _         <- userService.create(
                    pseudo       = Pseudo("Chris"),
                    emailHash    = emailHash ,
                    pwHash       = pwHash,
                    introduction = "Feels good to be the Boss !")
    _        <- authService.authenticate(emailHash, pwHash)
  } yield ()

  private[this] def elisa(authService: AuthService[IO], userService: UserService[IO]): IO[Unit] = for {
    emailHash <- IO.fromOption(Hash.md5("elisa@green-gen.org"))(fail("Couldn't hash email"))
    pwHash    <- IO.fromOption(Hash.md5("gogogo"))(fail("Couldn't hash password"))
    _         <- userService.create(
                    pseudo       = Pseudo("Elisa"),
                    emailHash    = emailHash ,
                    pwHash       = pwHash,
                    introduction = "Green General !")
    _         <- authService.authenticate(emailHash, pwHash)
  } yield ()

  private[this] def theblackcat(authService: AuthService[IO], userService: UserService[IO]): IO[Unit] = for {
    emailHash <- IO.fromOption(Hash.md5("blackcat@green-gen.org"))(fail("Couldn't hash email"))
    pwHash    <- IO.fromOption(Hash.md5("gogogo"))(fail("Couldn't hash password"))
    _         <- userService.create(
      pseudo       = Pseudo("TheCat"),
      emailHash    = emailHash ,
      pwHash       = pwHash,
      introduction = "Miaou !")
    _         <- authService.authenticate(emailHash, pwHash)
  } yield ()

  private[this] def docsquirrel(authService: AuthService[IO], userService: UserService[IO]): IO[Unit] = for {
    emailHash <- IO.fromOption(Hash.md5("docsquirrel@green-gen.org"))(fail("Couldn't hash email"))
    pwHash    <- IO.fromOption(Hash.md5("gogogo"))(fail("Couldn't hash password"))
    _         <- userService.create(
      pseudo       = Pseudo("DocSquirrel"),
      emailHash    = emailHash ,
      pwHash       = pwHash,
      introduction = "DocSquirrel can answer all your questions about our little furry friends and animals in general.\nNature lover and squirrel feeder.")
    _         <- authService.authenticate(emailHash, pwHash)
  } yield ()

  private[this] def userIdByPseudo(userService: UserService[IO], pseudo: String): IO[UserId] = for {
    allIds   <- userService.users()
    allUsers <- allIds.map(userService.byId(_)).sequence
    user     <- IO.fromOption(allUsers
                  .flatten
                  .find(_._2.pseudo.value==pseudo)
                  .map(_._1)) (fail(s"Pseudo $pseudo can't be found"))
  } yield user.id

  // Tips Helper

  private[this] def createTips(userService: UserService[IO],
                               tipService: TipService[IO]): IO[Unit] = for {
    elisaId       <- userIdByPseudo(userService, "Elisa")
    _             <- makeTip(tipService, elisaId, "Tip green-beauty from Elisa")
    _             <- makeTip(tipService, elisaId, "Another cool tip from Elisa")
    chrisId       <- userIdByPseudo(userService, "Chris")
    _             <- makeTip(tipService, chrisId, "My tip to share tips ... a lot !")
    _             <- makeTip(tipService, chrisId, "Don't forget the tip, you, bloody french !")
    catId         <- userIdByPseudo(userService, "TheCat")
    _             <- makeTip(tipService, catId, "Miaou !")
    _             <- makeTip(tipService, catId, "Don't eat electric wires !")
    docsquirrelId <- userIdByPseudo(userService, "DocSquirrel")
    _             <- makeTip(tipService, docsquirrelId,
      "When you throw them some nuts, you have to throw them on teh side, else they will not see it !")
    _             <- makeTip(tipService, docsquirrelId,
      "When squirrels are sleeping, they put their tail on the belly ! It keeps them warm :)")
  } yield ()

  private[this] def makeTip(tipService: TipService[IO],
                            author: UserId, content: String = "This is a great tip !"): IO[TipId] = for {
    tipId <- tipService.create(author, content, List(MySelf))
  } yield tipId

  // Polls Helper

  private[this] def createPolls(userService: UserService[IO],
                                pollService: PollService[IO]): IO[Unit] = for {
    elisaId       <- userIdByPseudo(userService, "Elisa")
    _             <- makePoll(pollService, elisaId, "I wonder if all that is useful. What do you think ?")
    chrisId       <- userIdByPseudo(userService, "Chris")
    _             <- makePoll(pollService, chrisId, "Do you like cats ?", List("Yes","No","Maybe"))
    catId         <- userIdByPseudo(userService, "TheCat")
    _             <- makePoll(pollService, catId, "Do you like Mice ?", List("Yes","No","A Lot !"))
    docsquirrelId <- userIdByPseudo(userService, "DocSquirrel")
    _             <- makePoll(pollService, docsquirrelId,
      "When squirrels are sleeping, they put their tail on the belly ! Did you know ?", List("Yes", "No","Amazing !"))
  } yield ()

  private[this] def makePoll(pollService: PollService[IO],
                             author: UserId,
                             title: String = "This is a great poll ! Do you think GreenGen is useful ?",
                             options: List[String] = List("Maybe", "Not sure", "We will see !", "Yes, great idea !")): IO[PollId] = for {
    pollId <- pollService.create(author, title, options.map(PollOption(_)))
  } yield pollId

  // Challenges Helper

  private[this] def createChallenges(userService: UserService[IO],
                                     challengeService: ChallengeService[IO]): IO[Unit] = for {
    success  <- IO(SuccessMeasure(maxFailure = 1, maxPartial = 1, maxSkip = 1))
    elisaId  <- userIdByPseudo(userService, "Elisa")
    _        <- makeChallenge(challengeService, elisaId,
      title ="One challenge a day, the global warming away !",
      content = "Global warming, go away !",
      success)
    chrisId  <- userIdByPseudo(userService, "Chris")
    _        <- makeChallenge(challengeService, chrisId,
      title = "One steak a day, death on its way !",
      content = "One steak a week no more - for the whole year",
      success)
    catId    <- userIdByPseudo(userService, "TheCat")
    _        <- makeChallenge(challengeService,
      catId,
      title = "Catch a mouse with my rear left leg",
      content = "... and blind-folded",
      success)
  } yield ()

  private[this] def makeChallenge(challengeService: ChallengeService[IO],
                                  author: UserId,
                                  title: String = "My daily Challenge",
                                  content: String = "This is a great green personal challenge !",
                                  successMeasure: SuccessMeasure): IO[ChallengeId] = for {
    challengeId <- challengeService.create(
      author,
      title,
      content,
      schedule = Schedule.daily(UTCTimestamp.plusDays(UTCTimestamp.now(), -7), UTCTimestamp.plusDays(UTCTimestamp.now(), 7)).get,
      successMeasure)
  } yield challengeId


  // Notifications

  private[this] def createNotifications(userService: UserService[IO],
                                        notificationService: NotificationService[IO]): IO[Unit] = for {
      _ <- notificationService.platform("This is a GreenGen announcement: tatadatadatada !!!")
    } yield ()

  // Helpers

  private[this] def fail(msg: String) =
    new RuntimeException(msg)

  private[this] def myself: Source =
    MySelf

  private[this] def now() =
    UTCTimestamp.now()

  private[this] def nextYear() =
    UTCTimestamp.now().plusMillis(365 * 24 * 3600 * 1000)

  private[this] def ht(words: String*) =
    words.map(Hashtag(_)).toSet
}
