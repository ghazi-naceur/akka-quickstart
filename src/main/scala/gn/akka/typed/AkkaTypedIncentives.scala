package gn.akka.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

object AkkaTypedIncentives {
  // Akka Typed is praised for bringing compile-time checks to Actors, and a whole Actor API.
  // The messages that an Actor can receive will be reflected in the Actor itself and vice-versa. So the type of its Actor
  // Ref will be both a reason for compiler to warn you if you don't send the right messages of the right type, and also
  // an indication about the Actor is supposed to do

  // 1- typed messages & actors
  sealed trait ShoppingCartMessage // Using 'sealed', once you do the pattern matching, it needs to be exhaustive, otherwise
  // the compiler will warn you about that

  case class AddItem(item: String) extends ShoppingCartMessage
  case class RemoveItem(item: String) extends ShoppingCartMessage
  case object ValidateCart extends ShoppingCartMessage

  // In Akka Typed API, an Actor is defined by its behavior
  // ActorSystem is defined from "akka.actor.typed", not from "akka.actor"
  val shoppingRootActor: ActorSystem[ShoppingCartMessage] = ActorSystem(
    Behaviors.receiveMessage[ShoppingCartMessage] { message: ShoppingCartMessage =>
      message match {
        case AddItem(item) =>
          println(s"Adding $item to cart")
        case RemoveItem(item) =>
          println(s"Removing $item from cart")
        case ValidateCart =>
          println("The cart is good")
      }
      Behaviors.same
    },
    "simpleShoppingActor"
  )
//  shoppingRootActor ! "Hello! actor" // This won't compile, because the message that we attempt to send is a string,
  // but the compiler is expected a sub class of ShoppingCartMessage
  shoppingRootActor ! ValidateCart // It's good that the compiler is able to determine the message type, but the problem
  // here is that we're constraint with the message with type ShoppingCartMessage, but in real world we may have other types.
  // We can workaround this issue, but defining 'Any' as a return message type, instead of ShoppingCartMessage, but this
  // workaround can lead to an anti-pattern, and we can't control what we receive, and as a result we won't have a good
  // quality code

  // 2- mutable state
  // Mutable state and mutable actors are discouraged in the beginning of Akka, but variables and mutable states haven't
  // disappeared yet
  val shoppingRootActorMutable: ActorSystem[ShoppingCartMessage] = ActorSystem(
    Behaviors.setup[ShoppingCartMessage] { context =>
      // With 'setup', we're now allowed to perform some preliminary actions right here in this code section:
      // Local state = mutable
      var items: Set[String] = Set()
      Behaviors.receiveMessage[ShoppingCartMessage] { message: ShoppingCartMessage =>
        message match {
          case AddItem(item) =>
            println(s"Adding $item to cart")
            items += item
          case RemoveItem(item) =>
            println(s"Removing $item from cart")
            items -= item
          case ValidateCart =>
            println("The cart is good")
        }
        Behaviors.same
      }
    },
    "simpleShoppingActorMutable"
  )
  // The same code but with immutable state, using recursive function
  def shoppingBehavior(items: Set[String]): Behavior[ShoppingCartMessage] =
    Behaviors.receiveMessage[ShoppingCartMessage] {
      case AddItem(item) =>
        println(s"Adding $item to cart")
        shoppingBehavior(items + item)
      case RemoveItem(item) =>
        println(s"Removing $item from cart")
        shoppingBehavior(items - item)
      case ValidateCart =>
        println("The cart is good")
        Behaviors.same
    }

  // 3- hierarchy
//  Common anti-pattern in the old Akka API is 'System.actorOf'. It resulted a very flat Actor hierarchies.
//  In the new Akka API, you can't to spawn Actors. You can only spawn child Actors from a given Actor
//  val rootOnlineStoreActor: ActorSystem[ShoppingCartMessage] = ActorSystem(
//    Behaviors.setup { context =>
//      // Create children here
//      context.spawn(shoppingBehavior(Set()), "shoppingCart")
//      Behaviors.empty
//    },
//    "onlineStore"
//  )
}
