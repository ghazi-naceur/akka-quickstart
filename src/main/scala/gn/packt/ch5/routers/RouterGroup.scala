package gn.packt.ch5.routers

import akka.actor.{Actor, ActorSystem, Props}
import gn.packt.ch5.routers.Worker.Work

// There is another type of router, which is a 'group', that means 'routees' are created externally to the 'router'
// and the 'router' sends messages to the specified path using an Actor Selection
class RouterGroup(routees: List[String]) extends Actor {
  override def receive: Receive = {
    case msg: Work =>
      println("I'm a Router Group and I receive Work message...")
      context.actorSelection(routees(util.Random.nextInt(routees.size))) forward msg
  }
}

// # Second example
object RouterApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("router")
    system.actorOf(Props[Worker], "w1")
    system.actorOf(Props[Worker], "w2")
    system.actorOf(Props[Worker], "w3")
    system.actorOf(Props[Worker], "w4")
    system.actorOf(Props[Worker], "w5")

    val workers: List[String] = List("/user/w1", "/user/w2", "/user/w3", "/user/w4", "/user/w5")

    val routerGroup = system.actorOf(Props(classOf[RouterGroup], workers))

    routerGroup ! Work()
    routerGroup ! Work()

    Thread.sleep(100)
    system.terminate()
    // This implementation may produce a bottleneck is our app
  }
}
