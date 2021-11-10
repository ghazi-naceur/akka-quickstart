package gn.akka.streams

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, Graph}
import akka.stream.scaladsl.{
  Broadcast,
  Flow,
  GraphDSL,
  RunnableGraph,
  Sink,
  Source,
  Zip
}

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

  // Graph DSL
  // Step 1: Frame
  // .create is a carrier function, with empty parameters for the first param (to be explored later), and a second param as
  // a function (lambda expression) from an implicit builder as a GraphDSL
  val specialGraph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Step 2: Create the building blocs
      val input = builder.add(Source(1 to 1000))
      val incrementer = builder.add(Flow[Int].map(_ + 1)) // hard computation 1
      val multiplier = builder.add(Flow[Int].map(_ * 10)) // hard computation 2
      val output = builder.add(Sink.foreach[(Int, Int)](println))
      //  non-standard components
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int])

      // Step 3 : Glue the components together
      // input feeds into (~>) broadcast == the output of the source will be fed into the input of the broadcast
      // the implicit builder will be informed about the interaction between input and broadcast
      input ~> broadcast
      // broadcast.out(0) first output of broadcast and zip.in0 is first input
      broadcast.out(0) ~> incrementer ~> zip.in0
      // broadcast.out(1) second output of broadcast and zip.in1 is second input
      broadcast.out(1) ~> multiplier ~> zip.in1
      zip.out ~> output

      // Step 4: closing - returning a return value
//      ClosedShape is a singleton object which is a marker for Akka Steams to validate your graph when you instantiate
      ClosedShape
  }
  def main(args: Array[String]): Unit = {
    graph.run()

    RunnableGraph.fromGraph(specialGraph).run()
  }
}
