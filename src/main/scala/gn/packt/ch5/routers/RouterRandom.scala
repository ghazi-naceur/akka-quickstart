package gn.packt.ch5.routers

import akka.actor.{ActorSystem, Props}
import akka.routing.{FromConfig, RandomGroup}
import gn.packt.ch5.routers.Worker.Work

object RouterRandom {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Random-Router")
    val routerPool = system.actorOf(FromConfig.props(Props[Worker]), "random-router-pool")

    routerPool ! Work()
    routerPool ! Work()
    routerPool ! Work()
    routerPool ! Work()

    system.actorOf(Props[Worker], "w1")
    system.actorOf(Props[Worker], "w2")
    system.actorOf(Props[Worker], "w3")

    val paths: List[String] = List("/user/w1", "/user/w2", "/user/w3")

    val routerGroup = system.actorOf(RandomGroup(paths).props(), "random-router-group")

    routerGroup ! Work()
    routerGroup ! Work()
    routerGroup ! Work()
    routerGroup ! Work()

    Thread.sleep(100)
    system.terminate()
    // There are other random routers, like:
    //  - Round Robin Router
    //  - Balancing Router
    //  - Smallest Mailbox Router
    //  - Broadcast Router
    //  - Scatter Gather First Complete Router
    //  - Consistent Hashing Router
    //  - Tail Chopping Router
  }
}
