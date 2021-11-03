package gn.akka.quickstart

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import gn.akka.quickstart.OrderFirstProcessor.FPOrder
import gn.akka.quickstart.OrderFourthProcessor.FoPOrder
import gn.akka.quickstart.OrderSecondProcessor.SPOrder
import gn.akka.quickstart.OrderThirdProcessor.TPOrder
import gn.akka.quickstart.Shipper.Shipment

object OrderFirstProcessor {
  final case class FPOrder(
      id: Int,
      product: String,
      number: Int
  ) // the message to be sent
  // Akka actor needs to have an apply method that returns a Behavior of something/message
  def apply(): Behavior[FPOrder] = Behaviors.receiveMessage { message =>
    println(message.toString)
    Behaviors.same // to advise the system to reuse the previous behavior. This is provided in order to avoid the allocation overhead
  }
}

object OrderSecondProcessor {
  final case class SPOrder(
      id: Int,
      product: String,
      number: Int
  ) // the message to be sent
  // Akka actor needs to have an apply method that returns a Behavior of something/message
  def apply(): Behavior[SPOrder] = Behaviors.receive { (context, message) =>
    context.log.info("This is the received message '{}'.", message.toString)
    Behaviors.same // to advise the system to reuse the previous behavior. This is provided in order to avoid the allocation overhead
  }
}

object OrderThirdProcessor {
  final case class TPOrder(
      id: Int,
      product: String,
      number: Int
  ) // the message to be sent
  // Akka actor needs to have an apply method that returns a Behavior of something/message
  def apply(): Behavior[TPOrder] = Behaviors.setup { context =>
    // Setting up the Actor behavior .. here I'll get the message
    Behaviors.receiveMessage { message =>
      // The creation of the behavior is created when the actor actually starts
      context.log.info("This is the received message '{}'.", message.toString)
      Behaviors.same
    }
  }
}

object Shipper {
  final case class Shipment(orderId: Int, product: String, number: Int)

  def apply(): Behavior[Shipment] = Behaviors.receive { (context, message) =>
    context.log.info("This is the received shipment '{}'.", message.toString)
    Behaviors.same
  }
}

object OrderFourthProcessor {
  final case class FoPOrder(
      id: Int,
      product: String,
      number: Int
  ) // the message to be sent
  def apply(): Behavior[FoPOrder] = Behaviors.setup { context =>
    // OrderFourthProcessor will send order to Shipper
    val shipperRef = context.spawn(Shipper(), "shipper")
    Behaviors.receiveMessage { message =>
      context.log.info("This is the received order '{}'.", message.toString)
      shipperRef ! Shipment(message.id, message.product, message.number)
      Behaviors.same
    }
  }
}

object Main extends App {
  // Actors communicate via messages in asynchronous way
  // bootstrapping the ActorSystem => entrypoint for our orders
  val orderProcessor: ActorSystem[OrderFirstProcessor.FPOrder] =
    ActorSystem(OrderFirstProcessor(), "orders-1")
  // sending messages with ! which is an asynchronous non-blocking call == fire and forget call
  orderProcessor ! FPOrder(0, "chair", 3)
  orderProcessor ! FPOrder(1, "table", 2)
  orderProcessor ! FPOrder(2, "desk", 1)

  val orderSecondProcessor: ActorSystem[OrderSecondProcessor.SPOrder] =
    ActorSystem(OrderSecondProcessor(), "orders-2")
  // sending messages with ! which is an asynchronous non-blocking call == fire and forget call
  orderSecondProcessor ! SPOrder(3, "chair", 3)
  orderSecondProcessor ! SPOrder(4, "table", 2)
  orderSecondProcessor ! SPOrder(5, "desk", 1)

  val orderThirdProcessor: ActorSystem[OrderThirdProcessor.TPOrder] =
    ActorSystem(OrderThirdProcessor(), "orders-3")
  // sending messages with ! which is an asynchronous non-blocking call == fire and forget call
  orderThirdProcessor ! TPOrder(6, "chair", 3)
  orderThirdProcessor ! TPOrder(7, "table", 2)
  orderThirdProcessor ! TPOrder(8, "desk", 1)

  val orderFourthProcessor: ActorSystem[OrderFourthProcessor.FoPOrder] =
    ActorSystem(OrderFourthProcessor(), "orders-4")
  // sending messages with ! which is an asynchronous non-blocking call == fire and forget call
  orderFourthProcessor ! FoPOrder(9, "chair", 3)
  orderFourthProcessor ! FoPOrder(10, "table", 2)
  orderFourthProcessor ! FoPOrder(11, "desk", 1)
}
