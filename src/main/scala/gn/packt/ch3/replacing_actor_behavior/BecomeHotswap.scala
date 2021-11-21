package gn.packt.ch3.replacing_actor_behavior

import akka.actor.{Actor, ActorSystem, Props, Stash}
import gn.packt.ch3.replacing_actor_behavior.UserStorage.{Connect, Disconnect, Operation}

// 'become' and 'unbecome' allows to change the Actor's behavior at runtime
case class User(username: String, email: String)

object UserStorage {
  trait DBOperation
  object DBOperation {
    case object Create extends DBOperation
    case object Update extends DBOperation
    case object Read extends DBOperation
    case object Delete extends DBOperation
  }

  case object Connect
  case object Disconnect
  case class Operation(dBOperation: DBOperation, user: Option[User])
}

class UserStorageActor extends Actor with Stash {
// Stash trait enables the Actor to temporarely stash away messages, that should not be handled using the Actor current
// Behavior (for example, trying to perform a DBOperation before Connecting)
  def connected: Actor.Receive = {
    case Disconnect =>
      println("User storage disconnected from DB")
      context.unbecome()
    case Operation(operation, user) =>
      println(s"User storage receive '$operation' operation to do in user '$user'")
  }
  def disconnected: Actor.Receive = {
    case Connect =>
      println("User storage connected to DB")
      unstashAll() // remove/dequeue all messages from the Actor's stash to the Actor's mailbox
      context.become(connected)
    case _ =>
      stash() // add/enqueue the current message to the Actor's stash
  }
  override def receive: Receive = disconnected
}

object BecomeHotswap {
  def main(args: Array[String]): Unit = {
    import UserStorage._
    val system = ActorSystem("Hotswap-Become")
    val userStorage = system.actorOf(Props[UserStorageActor], "userStorage")

    userStorage ! Operation(DBOperation.Create, Some(User("Admin1", "admin1@admin.com")))
    userStorage ! Operation(DBOperation.Create, Some(User("Admin2", "admin2@admin.com")))
    userStorage ! Connect
    userStorage ! Operation(DBOperation.Create, Some(User("Admin3", "admin3@admin.com")))
    userStorage ! Disconnect

    Thread.sleep(1000)
    system.terminate()
  }
}
