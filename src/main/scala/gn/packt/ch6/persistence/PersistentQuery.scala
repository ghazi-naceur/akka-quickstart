package gn.packt.ch6.persistence

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.persistence.query.{EventEnvelope, PersistenceQuery}

object PersistentQuery {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("persistent-query")
    implicit val mat: ActorMaterializer = ActorMaterializer()(system)

    val queries = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

    val events: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId("account", 0L, 100L)

    events.runForeach(evt => println(s"Event: '$evt'"))
    Thread.sleep(1000)
    system.terminate()
  }
}
