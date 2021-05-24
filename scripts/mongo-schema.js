use greengen

// App user
db.createUser({
    user: "greengen",
    pwd: "gogogo",
    roles: [{ role: "readWrite", db: "greengen" }]
})

// Dropping everything
db.users.drop()
db.followers.drop()
db.wall.drop()
db.hashtags.drop()
db.posts.drop()
db.posts.pinned.drop()
db.posts.flagged.drop()
db.posts.likes.drop()
db.posts.tips.drop()
db.posts.challenges.drop()
db.posts.challenges.challenged.drop()
db.posts.challenges.reports.drop()

// Users
db.users.createIndex({"user_id": 1}, { unique: true })
db.users.createIndex({"credentials.email_hash": 1}, { unique: true })
db.users.createIndex({"profile.pseudo": 1}, { unique: true })
// Wall
db.wall.createIndex({"user_id": 1,"post_id": 1}, {unique: true})
// Posts
db.posts.createIndex({"post_id": 1}, { unique: true })
db.posts.createIndex({"author": 1})
db.posts.createIndex({"hashtags": 1})
db.posts.flagged.createIndex({"flagged_by": 1, "post_id": 1}, {unique: true})
db.posts.pinned.createIndex({"post_id": 1, "user_id": 1}, {unique: true})
// Post content
db.posts.tips.createIndex({"tip_id": 1})
db.posts.challenges.createIndex({"challenge_id": 1})
db.posts.events.createIndex({"event_id": 1})
db.posts.polls.createIndex({"poll_id": 1})
db.posts.challenges.challenged.createIndex({"challenge_id": 1, "challengee_id": 1}, { unique: true})
db.posts.challenges.reports.createIndex({"challenge_id": 1, "challengee_id": 1}, {unique: true})
// Likes
db.posts.likes.createIndex({"post_id": 1, "user_id": 1}, {unique: true})
// Followers
db.followers.createIndex({"user_id": 1, "follower_id": 1}, {unique: true})
// Hashtags
db.hashtags.createIndex({"hashtag": 1, "user_id": 1}, {unique: true})