package gn.akka.adapter

import akka.actor.typed.ActorRef

// https://www.youtube.com/watch?v=aVgjEMA1AEQ
object AkkaMessageAdaptation {

  object StoreDomain {
    // never use double for money - for illustration purposes
    case class Product(name: String, price: Double)
  }

  object ShoppingCart {
    import StoreDomain._
    sealed trait Request
    case class GetCurrentCart(cartId: String, replyTo: ActorRef[Response]) extends Request
    // + some others

    sealed trait Response
    case class CurrentCart(cartId: String, items: List[Product]) extends Response
    // + some others

  }

  object Checkout {
    import ShoppingCart._

    // this is what we receive from the customer
    sealed trait Request
    final case class InspectSummary(cartId: String, replyTo: ActorRef[Response]) extends Request
    // + some others
    // private because the customer actor doesn't to know about the existence of this class
    private final case class WrappedShoppingCartResponse(response: ShoppingCart.Response) extends Request
    // So wherever response we receive from ShoppingCart, we need to wrap those into this kind of message that the Checkout
    // actor needs to support

    // this is what we send to the customer
    sealed trait Response
    final case class Summary(cartId: String, amount: Double) extends Response
    // + some others
  }

  // customer -> Checkout -> ShoppingCart
  //            "frontend"    "backend"
  // Checkout Actor is in the middle, between the Customer and the ShoppingCart. It may have a Behavior similar to the
  // following: Behavior[Checkout.Request & ShoppingCart.Response] => In other terms, the Checkout Actor will receive
  // requests from the Customer and responses from the ShoppingCart, and this causes a problem, because it's an
  // anti-pattern, because eventually the Checkout can interacts with other Actors, and they need to respond back to the
  // Checkout Actor, and this Checkout actor will need to deal with these responses too.
  // The right pattern is: each actor needs to support its own "request" type and nothing else.
  // So in our case, if the Checkout actor needs to receive messages that are replies from the ShoppingCart actor, we need
  // to turn those replies into that kind of requests(as if from the customer) that the Checkout actor needs to support,
  // and the easiest way to do that is to wrap those responses into a checkout request infants

  def main(args: Array[String]): Unit = {}
}
