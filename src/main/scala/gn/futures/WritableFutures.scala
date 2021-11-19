package gn.futures

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

object WritableFutures {
  // Futures are inherently non-deterministic, in a sense that when you create a Future, its value will be evaluated in some thread
  // at some point of time without your control

  val aFuture: Future[Int] = Future {
    42
  }

  // Introducing Promises, which are some kind of controller or wrapper on a Future
  // 1- create Promise:
  val myPromise: Promise[String] = Promise[String]()
  // 2- extract its future
  val myFuture: Future[String] = myPromise.future
  // 3- consume the future
  val furtherProcessing: Future[String] = myFuture.map(_.toUpperCase())
  // 4- passing the promise to something else, for example the producer of the values
  def asyncCall(promise: Promise[String]): Unit = {
    promise.success("This is a value")
  }
  // 5- call the producer
  asyncCall(myPromise)
  // This function will fulfill the promise 'promise.success' with the value '"This is a value"'. When the promise then
  // contain this previous value, then the Future will fulfilled with the same value automatically '"This is a value"'.

  // Multi-threaded service
  object MyService {
    def produceValue(value: Int): String = s"Your value: ${value / 42}"
    def submitTask[A](actualArg: A)(function: A => Unit): Boolean = {
      true
    }
    // 'submitTask' will take the argument 'actualArg' and run the 'function' function on 'actualArg' at some point on time
    // without your control
  }

  /**
  def returnValue(value: Int): Future[String] =
    Future {
      MyService.produceValue(value)
      // is a wrong implementation, because spawning up the thread responsible for evaluating
      // this function is not up to you, but depends on the service, so you can not spawn the future yourself. So you need
      // to depend on the service to create the return type Future[String] ==> Using Promise
    }
    */

  def returnValue(yourArg: Int): Future[String] = {
    // 1- create the promise
    val aPromise = Promise[String]()
    // 5- invoke the producer
    MyService.submitTask(yourArg) { x: Int =>
      // 4- producer logic
      // invoking the deterministic production function
      val preciousValue = MyService.produceValue(x)
      // fulfill the promise
      aPromise.success(preciousValue)
    }
    // 2- extract the future
    aPromise.future
    // 3- someone will use my API method/my future
    // As the promise is fulfilled in step 4 'aPromise.success(preciousValue)', the consumer in step 3 will be unlocked
    // automatically with the 'preciousValue' that the service created
    // => This is how to create a connection between a consumer and an async producer
  }

  def main(args: Array[String]): Unit = {}
}
