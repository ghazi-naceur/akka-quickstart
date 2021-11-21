package gn.packt.ch6.persistence

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

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
        takeSnapshot
      case Evt(Decrement(count)) =>
        state = State(count = state.count - count)
        takeSnapshot
    }
  // Persistent receive on recovery mode
  override def receiveRecover: Receive = {
    case evt: Evt =>
      println(s"Counter receive '$evt' on recovering mode")
      updateState(evt)
    case SnapshotOffer(metadata, snapshot: State) =>
      println(s"Counter receive snapshot with data '$snapshot' on recovering mode")
      state = snapshot
    case RecoveryCompleted =>
      println("Recovery complete and now I'll switch to receiving mode")
  }

  // The Persistent Actor stops on the Recovery Mode, and a new message sent to the Persistent Actor during Recovery doesn't
  // interface with the replayed messages. They are cached and received by the Persistent Actor after the recovery phase
  // is completed. This can be disabled by returning 'Recovery.none' in the 'recovery' method od the Persistent Actor:
  /** override def recovery: Recovery = Recovery.none */

  // If you want some initialization done after the recovery is complete, you can handle the 'RecoveryCompleted' case

  // Persistent receive on normal mode
  override def receiveCommand: Receive = {
    case cmd @ Cmd(op) =>
      println(s"Counter receive '$cmd'")
      persist(Evt(op)) { evt =>
        updateState(evt)
      }
    // 'persist' persists the event asynchronously. It takes a sequence of events and handlers that will be called when
    // the persisting succeeds. It is guaranteed that no new command will be received by a Persistent Actor between the
    // call of the 'persist' method and the execution of its handler. This is achieved by stashing new commands and
    // unstashing them, when an event is persisted and handled, that mean the Persistent Actor doesn't receive a new command
    // until the event was persisted and handled.
    // There is another version of the 'persist' method, called 'persistAsync' which like the 'persist' method, but it can
    // receive a new command after a call to a 'persistAsync'.
    // So if you don't want to execute a new command after the effect of the current command, you should use 'persistAsync'.
    // When 'persist' fails, 'onPersistFailure' will be invoked. By default, it logs the errors and the Actor is always
    // stopped after this method has been invoked. In this case, the event may or may not have been saved, depending on
    // the type of the failure
    case "print" =>
      println(s"The current state of counter is '$state'")
    case SaveSnapshotSuccess(metadata) =>
      println("Save snapshot succeeded")
    case SaveSnapshotFailure(metadata, reason) =>
      println(s"Save snapshot failed, because of: ${reason.toString}")
  }

  def takeSnapshot = {
    if (state.count % 5 == 0) {
      saveSnapshot(state)
    }
  }
}

object Persistent {
  def main(args: Array[String]): Unit = {
    import PersistentCounter._

    val system = ActorSystem("persistent-actors")
    val counter = system.actorOf(Props[PersistentCounter])

    counter ! Cmd(Increment(3))
    counter ! Cmd(Increment(5))
    counter ! Cmd(Decrement(3))

    counter ! "print"

    Thread.sleep(1000)
    system.terminate()
  }
}
