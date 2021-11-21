package gn.packt.ch5.routers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import gn.packt.ch5.routers.Worker.Work

class Worker extends Actor {
  import Worker._
  override def receive: Receive = {
    case msg: Work =>
      println(s"I received Work message and My ActorRef is : $self")
  }
}

object Worker {
  case class Work()
}

class Router extends Actor {

  var routees: List[ActorRef] = _

  override def preStart(): Unit = {
    routees = List.fill(5)(context.actorOf(Props[Worker]))
  }
  override def receive: Receive = {
    case msg: Work =>
      println("I'm a Router and I received a message...")
      routees(util.Random.nextInt(routees.size)) forward msg
  }
}

// # First example
object Router {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("router")
    val router = system.actorOf(Props[Router])

    // 'router' actor is the parent for 'routees', this is called 'pool'
    router ! Work()
    router ! Work()
    router ! Work()

    Thread.sleep(100)
    system.terminate()
  }
}
