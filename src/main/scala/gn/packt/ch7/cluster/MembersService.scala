package gn.packt.ch7.cluster

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

class SimpleWorker extends Actor {
  import SimpleWorker._
  override def receive: Receive = {
    case msg: Work =>
      println(s"I received Work message and My ActorRef is: '$self'")
  }
}

object SimpleWorker {
  case class Work(message: String)
}

object MembersService {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load.getConfig("MembersService")
    val system = ActorSystem("MembersService", config)

    val worker = system.actorOf(Props[SimpleWorker], "remote-worker")
    println(s"Worker actor path is '${worker.path}'")
  }
}
// There are 2  types of remote interaction:
//  - Lookup: is used to lookup an actor on remote node with 'ActorSelection'
//  - Creations: is ued to create an actor on remote node with 'actorOf'
object MembersServiceLookup {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load.getConfig("MembersServiceLookup")
    val system = ActorSystem("MembersServiceLookup", config)

    val worker = system.actorSelection("akka://MembersService@127.0.0.1:25520/user/remote-worker")
    worker ! SimpleWorker.Work("Hi remote actor!")

  }
}
