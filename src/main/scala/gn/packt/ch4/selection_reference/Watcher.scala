package gn.packt.ch4.selection_reference

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, ActorSystem, Identify, Props}

class Watcher extends Actor {
  var counterRef: ActorRef = _
  val selection: ActorSelection = context.actorSelection("/user/counter")
  selection ! Identify(None) // Identify message contains the message ID that will return a response

  override def receive: Receive = {
    case ActorIdentity(messageId, Some(ref)) =>
      println(s"Actor Reference for counter is: '$ref'")
    case ActorIdentity(messageId, None) =>
      println(s"Actor Selection for Actor does not exist")
  }
}

object Watcher {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("watch-actor-selection")
    val counter = system.actorOf(Props[Counter], "counter")
    val watcher = system.actorOf(Props[Watcher], "Watcher")

    system.terminate()
  }
}
