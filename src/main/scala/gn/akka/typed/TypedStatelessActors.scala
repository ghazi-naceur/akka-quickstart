package gn.akka.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

// https://www.youtube.com/watch?v=gwZjdRQTPu8
object TypedStatelessActors {

  // The state of an Actor is not accessed directly, but we can only interact with Actors using asynchronous message
  // exchange. Handling these messages is thread-safe, which eliminate managing threads and concurrency => This helps
  // write massively distributed systems, so no locks needed and no general concurrency issues

  trait SimpleThing
  case object EatChocolate extends SimpleThing
  case object WashDishes extends SimpleThing
  case object LearnAkka extends SimpleThing

  // Stateful
  val emotionalMutableActor: Behavior[SimpleThing] = Behaviors.setup { context =>
    // spin up the actor state
    var happiness = 0
    // return the behavior of the actor
    Behaviors.receiveMessage {
      case EatChocolate =>
        context.log.info(s"$happiness ... eating chocolate")
        happiness += 1
        Behaviors.same
      case WashDishes =>
        context.log.info(s"$happiness ... washing dishes")
        happiness -= 2
        Behaviors.same
      case LearnAkka =>
        context.log.info(s"$happiness ... learning akka")
        happiness += 100
        Behaviors.same
      case _ =>
        context.log.info(s"$happiness ... receiving a weird message")
        Behaviors.same
    }
  }

  // Stateless
  // This function is not truly recursive, because when a thread schedules this actor for execution , it will dequeue the
  // messages from its own mailbox. Now once it handles the message, it will create a new behavior which is a new call of
  // the method 'emotionalFunctionalActor', but this method returns immediately with a new behavior object (the return type
  // Behavior[SimpleThing]), so it doesn't call itself forever, or recursively. So the thread will simply apply this new
  // return object to the actor, so that when the actor schedules again for execution and it handles again the next message,
  // that particular object will be the handler of the message, so this is not a true recursive call.. so don't be afraid
  // to call this method again, it will not cause a memory error
  def emotionalFunctionalActor(happiness: Int = 0): Behavior[SimpleThing] =
    Behaviors.receive { (context, message) =>
      message match {
        case EatChocolate =>
          context.log.info(s"$happiness ... eating chocolate")
          emotionalFunctionalActor(happiness + 1) // new behavior
        case WashDishes =>
          context.log.info(s"$happiness ... washing dishes")
          emotionalFunctionalActor(happiness - 2) // new behavior
        case LearnAkka =>
          context.log.info(s"$happiness ... learning akka")
          emotionalFunctionalActor(happiness + 100) // new behavior
        case _ =>
          context.log.info(s"$happiness ... receiving a weird message")
          Behaviors.same
      }
    }

  def main(args: Array[String]): Unit = {
    val emotionalActorSystem1 = ActorSystem(emotionalMutableActor, "EmotionalSystem")
    emotionalActorSystem1 ! EatChocolate
    emotionalActorSystem1 ! EatChocolate
    emotionalActorSystem1 ! EatChocolate
    emotionalActorSystem1 ! WashDishes
    emotionalActorSystem1 ! LearnAkka

    Thread.sleep(1000)
    emotionalActorSystem1.terminate()

    val emotionalActorSystem2 = ActorSystem(emotionalFunctionalActor(), "EmotionalSystem")
    emotionalActorSystem2 ! EatChocolate
    emotionalActorSystem2 ! EatChocolate
    emotionalActorSystem2 ! EatChocolate
    emotionalActorSystem2 ! WashDishes
    emotionalActorSystem2 ! LearnAkka

    Thread.sleep(1000)
    emotionalActorSystem2.terminate()
  }
}
