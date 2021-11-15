package gn.akka.streams

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.OverflowStrategy
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}

import scala.concurrent.Future

// https://www.youtube.com/watch?v=L5FAyCCWGL0
object AkkaStreamsBackupPressure {

  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "StreamsSystem") // Akka actors
  // Components of Akka streams:
  // Publisher(source) => Processor(flow) => Subscriber(sink)
  val source: Source[Int, NotUsed] = Source(1 to 1000) //emits 1000 elements asynchronously
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 10)
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val graph: RunnableGraph[NotUsed] = source.via(flow).to(sink)

  // Backpressure: One of the fundamental features of Reactive Streams. Elements flow as response to demand from consumers
  // Fast consumers(subscribers): all is well
  // Slow consumers(subscribers): problem
  //      - consumer will send a signal(backpressure signal) to producer to slow down
  val slowSink: Sink[Int, Future[Done]] = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(x)
  }

  val debuggingFlow: Flow[Int, Int, NotUsed] = Flow[Int].map { x =>
    println(s"[Flow] $x")
    x
  }

  def demoNoBackpressure(): Unit = {
    // "via" and "to" allows us to have "fusion", which means that all these components actually run on the same Actor
    // behind the scenes
    // This is not backpressure, because source, flow and sink run in the same Actor
    // All elements will be processed sequentially, and each element will take 1s to be processed (Thread.sleep(1000)
    // inside the slowSink)
    source.via(debuggingFlow).to(slowSink).run()
  }

  //  In the case that one of the components in your Akka streams is slow, which breaks the assumption that the Akka streams
  // API falls by default, we could insert asynchronous boundry in-between any 2 components in [source - flow - sink], so
  // that you can run them in separate actors

  def demoBackpressure(): Unit = {
    // .async which means that up to this point everything will run in a separate Actor
    // after .async, the rest run in a separate Actor as well
    // All Akka streams components, if they run in separate Actors, have internal buffers, so that they maintain the
    // throughput of the final stream, which will then be cleared once the consumer starts behaving a little bit faster.
    // In our case, the 'slowSink' will force the 'debuggingFlow', because they run in the same Actor, or force the
    // 'debuggingFlow' to buffer the 16 first elements, that the flow receives very quickly, and once the flow receives
    // 16 elements which is the total amount elements that can store by default, so every Akka stream component will have
    // 16 spaces by default, then the 'debuggingFlow' will consider that the buffer is full, and only then it will signal
    // a backpressure to the source. So the 'debuggingFlow' will attempt to maintain the throughput of the stream by
    // buffering elements in the meantime, and when the buffer is full then will have no choice, but to signal a backpressure
    // to the source, which is why is will stop producing more elements

    // There are some techniques/decisions that you can make in the case that the buffer of a component is full, and the
    // default behavior of a buffer full is to send backpressure signal upstream(to the source), which will slow down the
    // entire stream. There are some alternatives if you still want to maintain the original throughput from the producer,
    // if you can't slow it down for instance, you have to loose some data

    source.via(debuggingFlow).async.to(slowSink).run()
  }

  def demoBackpressure2(): Unit = {
//    source.via(debuggingFlow.buffer(16, OverflowStrategy.backpressure)).async.to(slowSink).run()
    // The "debuggingFlow.buffer(16, OverflowStrategy.backpressure)" are the default arguments for every component
    // There are alternatives if you can't slow the source, you can loose some data:
    source.via(debuggingFlow.buffer(10, OverflowStrategy.dropHead)).async.to(slowSink).run()
    // "OverflowStrategy.dropHead" will always drop the oldest element in the buffer to make room for the incoming one
    // Example:
    // [1 2 3 4 5 6 7 8 9 10] and 11 comes :: result => 1 is the oldest element, so the element 11 will take the 1's place
    // result : [11 2 3 4 5 6 7 8 9 10]
    // the incoming element 12 will take 2's place ... etc

    // "OverflowStrategy.dropHead" will drop the newest element to make room for the incoming element
    // "OverflowStrategy.dropBuffer" will drop the whole buffer if it's full when receiving an incoming element
    // "OverflowStrategy.dropNew" will drop the incoming element, when the buffer is full
    // "OverflowStrategy.fail" will fail/throw an exception when buffer is full => destroying the whole stream

  }

  def main(args: Array[String]): Unit = {
//    graph.run()
//    demoNoBackpressure()
//    demoBackpressure()
    demoBackpressure2()
  }
}
