package gn.packt.ch1.actor

import akka.actor.{Actor, ActorSystem, Props}

// Defining the Actor message
case class WhoToGreet(who: String)

// Defining Greeter Actor
class Greeter extends Actor {
  override def receive: Receive = {
    case WhoToGreet(who) => println(s"Hello $who")
  }
}

object HelloWorldAkka {

  def main(args: Array[String]): Unit = {
    // Create the hello akka Actor system
    val system = ActorSystem("Hello-Akka")
    // Create the greeter actor
    val greeter = system.actorOf(Props[Greeter], "greeter")
    // Send WhoToGreet message to actor
    greeter ! WhoToGreet("Akka")
  }
}
