use greengen

// Users
db.users.drop()
db.createUser({
    user: "greengen",
    pwd: "gogogo",
    roles: [{ role: "readWrite", db: "greengen" }]
})

db.users.createIndex({"user_id": 1}, { unique: true })
db.users.createIndex({"credentials.email_hash": 1}, { unique: true })
db.users.createIndex({"profile.pseudo": 1}, { unique: true })
//db.users.insert({
//    "user_id": "2afbe95a-754f-4a47-8968-87cb691141bd",
//    "credentials": {
//        "email_hash": "abf9a7ffe6bebf0db599ce6821ef09c0",
//        "password_hash": "35af4bf130805f0b86b1b13e49c8101e"
//    },
//    "enabled": true,
//    "profile": {
//        "pseudo": "chris",
//        "intro" : "The Boss !",
//        "since" : 1621364120238,
//        "verified": true
//    }
//})


