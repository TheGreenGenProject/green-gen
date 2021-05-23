use greengen

// App user
db.createUser({
    user: "greengen",
    pwd: "gogogo",
    roles: [{ role: "readWrite", db: "greengen" }]
})

// Dropping everything
db.users.drop()
db.likes.drop()
db.posts.drop()
db.posts.flagged.drop()
db.posts.challenges.drop()
db.posts.challenges.challenged.drop()
db.posts.challenges.reports.drop()

// Users
db.users.createIndex({"user_id": 1}, { unique: true })
db.users.createIndex({"credentials.email_hash": 1}, { unique: true })
db.users.createIndex({"profile.pseudo": 1}, { unique: true })

// Posts
db.posts.createIndex({"post_id": 1}, { unique: true })
db.posts.createIndex({"author": 1})
db.posts.createIndex({"hashtags": 1})

// Post content
db.posts.tips.createIndex({"tip_id": 1})
db.posts.challenges.createIndex({"challenge_id": 1})
db.posts.events.createIndex({"event_id": 1})
db.posts.polls.createIndex({"poll_id": 1})

db.posts.tips.insert({
    "tip_id": "2afbe95a-754f-4a47-888-87cb691141bd",
    "title": "This is a title",
    "content": "This is the content of a free text post"
)}
db.posts.challenges.insert({
    "challenge_id": "2afbe95a-754f-4a47-888-87cb691141bd",
    "title": "This is a title",
    "content": "This is the content of a free text post",
    "schedule": {
        "start": 1621364129863,
        "duration": 4129863,
        "every": 4129863,
        "end": 1621364129863
    },
    "success_measure": {
        "max_failure": 1,
        "max_skip": 2,
        "max_partial": 2,
    }
});

db.posts.challenges.challenged.createIndex({"challenge_id": 1, "challengee_id": 1}, { unique: true})
db.posts.challenges.challenged.insert({
    "challenge_id": "2afbe95a-754f-4a47-888-87cb691141bd",
    "challengee_id": "2afbe95a-754f-4a47-888-87cb691141bd",
    "timestamp": 1621364129863,
    "status": "Rejected"
})

db.posts.challenges.reports.createIndex({"challenge_id": 1, "challengee_id": 1}, {unique: true})
db.posts.challenges.reports.insert({
    "challenge_id": "2afbe95a-754f-4a47-888-87cb691141bd",
    "challengee_id": "2afbe95a-754f-4a47-888-87cb691141bd",
    "reports": [
        {
            "step": 1,
            "timestamp": 1621364129863,
            "status": "Success"
        },
        {
            "step": 3,
            "timestamp": 1621365555863,
            "status": "Failure"
        },
    ]
})

// Post flagged
db.posts.flagged.createIndex({"flagged_by": 1})
db.posts.flagged.createIndex({"post_id": 1})
db.posts.flagged.insert({
    "post_id": "2afbe95a-754f-4a47-8968-87cb691141bd",
    "user_id": "2afbe95a-754f-4a47-8968-87cb691141bd",
    "timestamp": 1621364120238,
    "reason": "for no reason !"});

// Likes
db.likes.createIndex({"post_id": 1})
db.likes.createIndex({"user_id": 1})
db.likes.insert({
    "post_id": "2afbe95a-754f-4a47-8888-87cb691141bd",
    "user_id": "1bfbe95a-754f-4a47-2222-87cb691141bd"
})