package gn.akka.sync_async_non_blocking

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

object AsyncNonBlocking {

  // 1- synchronous blocking
  def syncBlockingFunction(x: Int): Int = {
    Thread.sleep(10000)
    x + 42
  }
  syncBlockingFunction(5)
  // A blocking call, because the next statement 'meaningOfLife...' will wait 10 seconds to be executed
  val meaningOfLife = 42 // will wait 10 seconds before evaluating

  // 2- asynchronous blocking
  def asyncBlockingFunction(x: Int): Future[Int] =
    Future {
      Thread.sleep(10000)
      x + 42
    }
  asyncBlockingFunction(5)
  // will be evaluating in another thread, but still blocking because that thread will idle for 10 seconds
  val anotherMeaningOfLife = 42 // evaluates immediately

  // 3- asynchronous non-blocking: using Akka Actors
  def createSimpleActor(): Behaviors.Receive[String] =
    Behaviors.receiveMessage[String] { message =>
      println(s"Receiving message: $message")
      Behaviors.same
    }
  val rootActor: ActorSystem[String] = ActorSystem(createSimpleActor(), "TestSystem")
  rootActor ! "This is a message" // A thread will be just enqueuing a message = asynchronous action and non-blocking
  // A thread which is backing this ActorSystem will actually schedule this Actor for execution and a thread will take control
  // of this Actor and will start dequeuing messages from its mailbox and will run this lambda '{ message => ...'
  // in sequence for every single message in this actor's mailbox

  val promiseResolver: ActorSystem[(String, Promise[Int])] = ActorSystem(
    Behaviors.receiveMessage[(String, Promise[Int])] {
      case (message, promise) =>
        promise.success(message.length)
        Behaviors.same
    },
    "PromiseResolver"
  )

  def doAsyncNonBlockingComputation(s: String): Future[Int] = {
    val aPromise = Promise[Int]()
    promiseResolver ! (s, aPromise)
    aPromise.future
  }

  val asyncNonBlockingResult: Future[Int] = doAsyncNonBlockingComputation("Some message")
  // Future[Int] - async and non-blocking
  asyncNonBlockingResult.onComplete(println)

  def main(args: Array[String]): Unit = {}
}
