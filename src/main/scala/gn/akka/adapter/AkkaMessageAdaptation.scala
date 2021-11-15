package gn.akka.adapter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import gn.akka.adapter.AkkaMessageAdaptation.Checkout.Summary

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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
    val db: Map[String, List[Product]] = Map(
      "123" -> List(Product("chair", 50.0), Product("table", 100.0)),
      "234" -> List(Product("table", 100.0), Product("sofa", 400.0)),
      "345" -> List(Product("sofa", 400.0))
    )

    def dummy(): Behavior[Request] =
      Behaviors.receiveMessage {
        case GetCurrentCart(cartId, replyTo) =>
          replyTo ! CurrentCart(cartId, db(cartId))
          Behaviors.same
      }
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
    // shoppingCart: The Actor instance that will power ShoppingCart
    def apply(shoppingCart: ActorRef[ShoppingCart.Request]): Behavior[Request] = {
      Behaviors.setup { context =>
        // ActorRef will contain the message that we don't want to support, but we want to wrap, which is ShoppingCart.Response
        val messageAdaptor: ActorRef[ShoppingCart.Response] =
          context.messageAdapter(response => WrappedShoppingCartResponse(response))
        // The 'messageAdaptor' is like a fictitious actor that is receiving the 'ShoppingCart.Response' message and will
        // wrap it into the 'WrappedShoppingCartResponse' and send it back to the 'Checkout' Actor.
        // The solution is to insure that the 'Checkout' Actor is only responsible for message of type 'Checkout.Request'

        // "checkoutInProgress: Map[String, ActorRef[Response]]", 'String' is the Cart Id and 'ActorRef[Response]' is the
        // Actor to which we need to respond back with the state of the Checkout, which is the total amount they need to pay
        def handlingCheckouts(checkoutInProgress: Map[String, ActorRef[Response]]): Behavior[Request] =
          Behaviors.receiveMessage {
            case InspectSummary(cartId, customer) =>
              shoppingCart ! ShoppingCart.GetCurrentCart(cartId, messageAdaptor)
              handlingCheckouts(checkoutInProgress + (cartId -> customer))
            case WrappedShoppingCartResponse(response) =>
              // logic for dealing with response from ShoppingCart
              response match {
                case CurrentCart(cartId, items) =>
                  val summary = Summary(cartId, items.map(_.price).sum)
                  val customer = checkoutInProgress(cartId)
                  customer ! summary
                  handlingCheckouts(checkoutInProgress - cartId) // remove customer
              }
          }
        handlingCheckouts(Map())
      }
    }
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
  // The second part is to convert these instances of responses to 'Checkout.Request', in our case with 'WrappedShoppingCartResponse',
  // and Akka offers a first class API to do exactly that => apply method returning a Behavior

  def main(args: Array[String]): Unit = {
    import Checkout._
    val rootBehavior: Behavior[Any] = Behaviors.setup { context =>
      val shoppingCart = context.spawn(ShoppingCart.dummy(), "ShoppingCart")
      val customer = context.spawn(
        Behaviors.receiveMessage[Checkout.Response] {
          case Summary(_, amount) =>
            println(s"Total to pay: $amount")
            Behaviors.same
        },
        "Customer"
      )
      val checkout = context.spawn(Checkout(shoppingCart), "Checkout")

      // start interaction
      checkout ! InspectSummary("123", customer)

      // Not important
      Behaviors.empty
    }

    // setup the actor system
    val system = ActorSystem(rootBehavior, "main-app")
    implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())
    system.scheduler.scheduleOnce(1.second, () => system.terminate())
  }
}
