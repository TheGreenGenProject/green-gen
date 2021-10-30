package org.greengen.db.mongo

import org.mongodb.scala.{MongoClient, MongoDatabase}


object Database {

  val DatabaseName = "greengen"

  def connection(url: String): MongoDatabase = {
    System.setProperty("org.mongodb.async.type", "netty")
    val client = MongoClient(url)
    client.getDatabase(DatabaseName)
  }



}
