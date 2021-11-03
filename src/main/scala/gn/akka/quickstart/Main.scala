package gn.akka.quickstart

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import gn.akka.quickstart.OrderProcessor.Order

object OrderProcessor {
  final case class Order(id: Int, product: String, number: Int) // the message to be sent
  // Akka actor needs to have an apply method that returns a Behavior of something/message
  def apply(): Behavior[Order] = Behaviors.receiveMessage { message =>
    println(message.toString)
    Behaviors.same // to advise the system to reuse the previous behavior. This is provided in order to avoid the allocation overhead
  }
}

object OrderSecondProcessor {
  final case class Order(id: Int, product: String, number: Int) // the message to be sent
  // Akka actor needs to have an apply method that returns a Behavior of something/message
  def apply(): Behavior[Order] = Behaviors.receive { (context, message) =>
    context.log.info("This is the received message '{}'.", message.toString)
    Behaviors.same // to advise the system to reuse the previous behavior. This is provided in order to avoid the allocation overhead
  }
}

object Main extends App {
  // Actors communicate via messages in asynchronous way
  // bootstrapping the ActorSystem => entrypoint for our orders
  val orderProcessor: ActorSystem[OrderProcessor.Order] = ActorSystem(OrderProcessor(), "actors")
  // sending messages with ! which is an asynchronous non-blocking call == fire and forget call
  orderProcessor ! Order(0, "chair", 3)
  orderProcessor ! Order(1, "table", 2)
  orderProcessor ! Order(2, "desk", 1)
  orderProcessor ! Order(3, "sofa", 1)
  orderProcessor ! Order(4, "bed", 2)

  val orderSecondProcessor: ActorSystem[OrderProcessor.Order] = ActorSystem(OrderProcessor(), "actors")
  // sending messages with ! which is an asynchronous non-blocking call == fire and forget call
  orderSecondProcessor ! Order(5, "chair", 3)
  orderSecondProcessor ! Order(6, "table", 2)
  orderSecondProcessor ! Order(7, "desk", 1)
  orderSecondProcessor ! Order(8, "sofa", 1)
  orderSecondProcessor ! Order(9, "bed", 2)
}