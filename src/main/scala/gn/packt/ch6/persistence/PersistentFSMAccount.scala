package gn.packt.ch6.persistence

import akka.actor.{ActorSystem, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState

import scala.reflect.{ClassTag, classTag}

object Account {

  // Account states
  sealed trait State extends FSMState

  case class Empty() extends State {
    override def identifier: String = "Empty"
  }
  case class Active() extends State {
    override def identifier: String = "Active"
  }

  // Account data
  sealed trait Data {
    val amount: Float
  }

  case object ZeroBalance extends Data {
    override val amount: Float = 0.0f
  }

  case class Balance(override val amount: Float) extends Data

  // Domain events (Persistent events)
  sealed trait DomainEvent

  case class AcceptedTransaction(amount: Float, `type`: TransactionType) extends DomainEvent
  case class RejectedTransaction(amount: Float, `type`: TransactionType, reason: String) extends DomainEvent

  // Transaction types
  sealed trait TransactionType

  case object Credit extends TransactionType
  case object Debit extends TransactionType

  // Commands
  case class Operation(amount: Float, `type`: TransactionType)
}

// use EventSourcedBehavior instead
class Account extends PersistentFSM[Account.State, Account.Data, Account.DomainEvent] {
  import Account._
  override def persistenceId: String = "account"

  override def applyEvent(domainEvent: DomainEvent, currentData: Data): Data = {
    domainEvent match {
      case AcceptedTransaction(amount, Credit) =>
        val newAmount = currentData.amount + amount
        println(s"Your balance is: $newAmount")
        Balance(newAmount)
      case AcceptedTransaction(amount, Debit) =>
        val newAmount = currentData.amount - amount
        println(s"Your balance is: $newAmount")
        if (newAmount > 0)
          Balance(newAmount)
        else
          ZeroBalance
      case RejectedTransaction(amount, _, reason) =>
        println(s"Transaction rejected due to: $reason")
        currentData
    }
  }

  override def domainEventClassTag: ClassTag[DomainEvent] = classTag[DomainEvent]

  startWith(Empty(), ZeroBalance)

  when(Empty()) {
    case Event(Operation(amount, Credit), _) =>
      println("It's your first Credit Operation")
      goto(Active()) applying AcceptedTransaction(amount, Credit)
    case Event(Operation(amount, Debit), _) =>
      println("Your account has zero balance")
      stay applying RejectedTransaction(amount, Debit, "Balance is zero")
  }
  when(Active()) {
    case Event(Operation(amount, Credit), _) =>
      stay applying AcceptedTransaction(amount, Credit)
    case Event(Operation(amount, Debit), balance) =>
      val newBalance = balance.amount - amount
      if (newBalance > 0)
        stay applying AcceptedTransaction(amount, Debit)
      else if (newBalance == 0)
        goto(Empty()) applying AcceptedTransaction(amount, Debit)
      else
        stay applying RejectedTransaction(amount, Debit, "Balance is inferior to zero")
  }
}

object PersistentFSMAccount {
  def main(args: Array[String]): Unit = {
    import Account._

    val system = ActorSystem("persistent-fsm-actors")
    val account = system.actorOf(Props[Account])

    account ! Operation(1000, Credit)
    account ! Operation(10, Debit)

    Thread.sleep(1000)
    system.terminate()
  }
}
