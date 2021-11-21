package gn.packt.ch4.selection_reference

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}

class Counter extends Actor {
  import Counter._

  var count = 0

  override def receive: Receive = {
    case Inc(x) => count += x
    case Dec(x) => count -= x
  }
}

object Counter {
  final case class Inc(num: Int)
  final case class Dec(num: Int)
}

object ActorPath {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Actor-Paths")

    val counter1 = system.actorOf(Props[Counter], "Counter")
    println(s"Actor reference for counter1: '$counter1'")
    val counterSelection1 = system.actorSelection("counter")
    println(s"Actor Selection for counter1: '$counterSelection1'")

    counter1 ! PoisonPill // Killing our counter instance
    Thread.sleep(100)

    val counter2 = system.actorOf(Props[Counter], "Counter")
    println(s"Actor reference for counter2: '$counter2'")
    val counterSelection2 = system.actorSelection("counter")
    println(s"Actor Selection for counter2: '$counterSelection2'")

    // Result: Not the same Actor reference, but the same Actor selection (which a representation for a path, so it can
    // be the same, even when the Actor is killed, if the new one is created under the same path. In the other hand, the
    // reference is linked to specific actor instance)
    system.terminate()

  }
}
