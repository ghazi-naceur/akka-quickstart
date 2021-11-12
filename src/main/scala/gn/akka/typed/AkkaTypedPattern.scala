package gn.akka.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AkkaTypedPattern {

  // How we can pipe things to an actor == handling results of asynchronous computation == Actor encapsulation
  // Encapsulation = a state of an actor, which is an entity that is scheduled to run a thread at some point
  //    The state of this actor is inaccessible from the outside, even in a multithreaded, distributed environment
  //    You can only interact with this actor via a message exchange, however in real life, actors that we create might not
  //    necessarily bloc on resources when handling a message, they might call asynchronous services, and these services can
  //    break actor encapsulation, because handling an asynchronous response happens on some thread, which is potentially a
  //    different thread that the one which control the actor

//  This infrastructure object will expose an asynchronous service, that will access a bunch of data from fictitious database
  object Infrastructure {
    // This ExecutionContext will serve as a platform for running Futures
    private implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

    private val db: Map[String, Int] =
      Map("Isaac" -> 125, "Shisui" -> 29, "Takamora" -> 31)

    def asyncRetrievePhoneNumber(name: String): Future[Int] = Future(db(name))
  }

  trait PhoneCallProtocol
  case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol

  val phoneCallInitiatorV1: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    // Thread pool baked by the Akka Actor system
    implicit val ec: ExecutionContext = context.system.executionContext
    Behaviors.receiveMessage {
      case FindAndCallPhoneNumber(name) =>
        val futureNumber: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
        futureNumber.onComplete { // happens on another thread. This thread may not be the one handling this message
          case Success(number) =>
            //perform phone call
            context.log.info(s"Initiating phone call for $number")
            nPhoneCalls += 1 // a race condition because this a mutable variable that can be changed by multiple threads
          // In this case, we have broken the Actor encapsulation, because we have race conditions
          // The second problem is changing the sate of a mutable variable in a future call, we have
          // no choice, but to make this actor stateful with variables (var) ==> This problem can be
          // solved by the Pipe pattern
          case Failure(exception) =>
            context.log.error(s"Phone call failed for $name: $exception")
            nFailures += 1 // notes for "nPhoneCall += 1" are applied here as well
        }
        Behaviors.same
    }
  }

  // Pipe pattern = is the ability to forward the result of a Future back to me as a message == atomic
  // The return type will not be anymore "Future[Int]", but "Future[Something of type PhoneCallProtocol]" that can be accepted by the Actor
  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol
  case class LogPhoneCallFailure(reason: Throwable) extends PhoneCallProtocol

  val phoneCallInitiatorV2: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    // Thread pool baked by the Akka Actor system
    implicit val ec: ExecutionContext = context.system.executionContext
    Behaviors.receiveMessage {
      case FindAndCallPhoneNumber(name) =>
        val futureNumber: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
        context.pipeToSelf(futureNumber) { // Fully thread-safe, so no resource leaks or race conditions
          // partial function that will transform the Try[Int] to PhoneCallProtocol
          case Success(number) =>
            InitiatePhoneCall(number) // This message will be sent to me later
          case Failure(thr) =>
            LogPhoneCallFailure(thr) // This message will be sent to me later
        }
        Behaviors.same
      case InitiatePhoneCall(number) =>
        context.log.info(s"Initiating phone call for $number")
        nPhoneCalls += 1 // state mutation happens as a message handler, so no more race condition here
        Behaviors.same
      case LogPhoneCallFailure(thr) =>
        context.log.error(s"Phone call failed : $thr")
        nFailures += 1 // state mutation happens as a message handler, so no more race condition here
        Behaviors.same
    }
  }

  // The pipe pattern enables up to make the Actor stateless, because the mutable state changes in a message handler
  // (not a race condition anymore), so we can refactor the Actor in a method
  def phoneCallInitiatorV3(nPhoneCalls: Int = 0, nFailures: Int = 0): Behavior[PhoneCallProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case FindAndCallPhoneNumber(name) =>
          val futureNumber: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
          context.pipeToSelf(futureNumber) { // Fully thread-safe, so no resource leaks or race conditions
            // partial function that will transform the Try[Int] to PhoneCallProtocol
            case Success(number) =>
              InitiatePhoneCall(number) // This message will be sent to me later
            case Failure(thr) =>
              LogPhoneCallFailure(thr) // This message will be sent to me later
          }
          Behaviors.same
        case InitiatePhoneCall(number) =>
          context.log.info(s"Initiating phone call for $number")
          phoneCallInitiatorV3(nPhoneCalls + 1, nFailures)
        case LogPhoneCallFailure(thr) =>
          context.log.error(s"Phone call failed : $thr")
          phoneCallInitiatorV3(nPhoneCalls, nFailures + 1)
      }
    }

  def main(args: Array[String]): Unit = {

    val root1 = ActorSystem(phoneCallInitiatorV1, "PhoneCaller1")
    root1 ! FindAndCallPhoneNumber("Isaac")
    root1 ! FindAndCallPhoneNumber("Ippo")

    Thread.sleep(1000)
    root1.terminate()

    val root2 = ActorSystem(phoneCallInitiatorV2, "PhoneCaller2")
    root2 ! FindAndCallPhoneNumber("Shisui")
    root2 ! FindAndCallPhoneNumber("Itachi")

    Thread.sleep(1000)
    root2.terminate()

    val root3 = ActorSystem(phoneCallInitiatorV2, "PhoneCaller3")
    root3 ! FindAndCallPhoneNumber("Takamora")
    root3 ! FindAndCallPhoneNumber("Ging")

    Thread.sleep(1000)
    root3.terminate()
  }
}
