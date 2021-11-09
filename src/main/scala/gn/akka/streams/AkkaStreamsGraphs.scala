package gn.akka.streams

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}

import scala.concurrent.Future

object AkkaStreamsGraphs {

  // Creating potentially asynchronous components that deal with receiving or passing data around and plugin these components as pipes

  // untyped actor without a behavior
  implicit val system: ActorSystem = ActorSystem()

// Level 1 Akka streams:
  // streaming element from 1 to 1000 in order
  val source: Source[Int, NotUsed] = Source(1 to 1000)
  // transform elements inside flow and emit result: Transformer
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x * 2)
  // a Sink: a receiver of elements
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  // in order to run an actual application, you need to plug all these components like pipes:
  val graph: RunnableGraph[NotUsed] = source.via(flow).to(sink)

// Exercise: source fo ints => 2 independent "hard" computations => stitch the result to a tuple => print the tuples to the console

  def main(args: Array[String]): Unit = {
    graph.run()
  }
}
