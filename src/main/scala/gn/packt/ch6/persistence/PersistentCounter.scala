package gn.packt.ch6.persistence

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

object PersistentCounter {
  sealed trait Operation {
    val count: Int
  }
  case class Increment(override val count: Int) extends Operation
  case class Decrement(override val count: Int) extends Operation

  case class Cmd(operation: Operation)
  case class Evt(operation: Operation)

  case class State(count: Int)
}

class PersistentCounter extends PersistentActor with ActorLogging {
  import PersistentCounter._

  println("Starting...")

  // Persistent identifier
  override def persistenceId: String = "counter-example"

  var state: State = State(count = 0)

  def updateState(evt: Evt): Unit =
    evt match {
      case Evt(Increment(count)) =>
        state = State(count = state.count + count)
      case Evt(Decrement(count)) =>
        state = State(count = state.count - count)
    }
  // Persistent receive on recovery mode
  override def receiveRecover: Receive = {
    case evt: Evt =>
      println(s"Counter receive '$evt' on recovering mode")
      updateState(evt)
    case SnapshotOffer(metadata, snapshot: State) =>
      println(s"Counter receive snapshot with data '$snapshot' on recovering mode")
      state = snapshot
  }

  // Persistent receive on normal mode
  override def receiveCommand: Receive = {
    case cmd @ Cmd(op) =>
      println(s"Counter receive '$cmd'")
      persist(Evt(op)) { evt =>
        updateState(evt)
      }

    case "print" =>
      println(s"The current state of counter is '$state'")
  }
}

object Persistent {
  def main(args: Array[String]): Unit = {
    import PersistentCounter._

    val system = ActorSystem("persistent-actors")
    val counter = system.actorOf(Props[PersistentCounter])

    counter ! Cmd(Increment(3))
    counter ! Cmd(Increment(5))
    counter ! Cmd(Increment(3))

    counter ! "print"

    Thread.sleep(1000)
    system.terminate()
  }
}
