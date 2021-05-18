package org.greengen.db.mongo

import org.mongodb.scala.{MongoClient, MongoDatabase}


object Database {

  val DatabaseName = "greengen"

  def connection(url: String,
                 user: String,
                 password: String): MongoDatabase = {
    val client = MongoClient()
    client.getDatabase(DatabaseName)
  }



}
