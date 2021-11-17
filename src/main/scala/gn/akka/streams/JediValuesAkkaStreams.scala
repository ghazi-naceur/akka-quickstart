package gn.akka.streams

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future

// https://www.youtube.com/watch?v=2-CK76cPB9s
object JediValuesAkkaStreams {

  // Akka Streams is an implementation of the specification of Reactive Streams
  // High throughput and fault-tolerance streams of data, by simply plugging streaming components
  // sources, sinks and flows
  // Akka Streams run on top of Actors, so we need Actor system
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher
  val source: Source[Int, NotUsed] = Source(1 to 1000)
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 2)
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

  val graph: RunnableGraph[NotUsed] = source.via(flow).to(sink)

  val anotherGraph: RunnableGraph[Future[Done]] =
    source.via(flow).toMat(sink)((leftJediValue, rightJediValue) => rightJediValue)
  val sameAsAnotherGraph: RunnableGraph[Future[Done]] = source.via(flow).toMat(sink)(Keep.right)

  // Extracting the sum of all values that go through the stream:
  val summingSink: Sink[Int, Future[Int]] =
    Sink.fold[Int, Int](0)((currentSum, incomingElement) => currentSum + incomingElement)
//  val summingSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  def main(args: Array[String]): Unit = {
//    val jediValue: NotUsed = graph.run()
    // NotUsed: It is a combination of Scala’s `Unit` and Java’s `Void`
    // All streaming components in Akka Streams have Jedi Values when plugged in a living graph
    // The jedi value in the left of the route (in our case, 'NotUsed'(jedi value) inside 'source'(left side of route)),
    // will be used as a return type for the graph

//    val anotherJediValue: Future[Done] = anotherGraph.run()
//    anotherJediValue.onComplete(_ => println("steaming is done"))
    // anotherJediValue with type 'Future[Done]' tells us the streaming done

//    val yetAnotherJediValue: Future[Done] = sameAsAnotherGraph.run()
//    yetAnotherJediValue.onComplete(_ => println("steaming is done"))

    val sumFuture = source.toMat(summingSink)(Keep.right).run()
    // returning the JediValue of the right element of the Graph,
    // which is the 'Future[Int]' of 'summingSink'
    sumFuture.foreach(println)

    // Once you start a stream, there is no turning back, so we use these Jedi Values to get info about the stream,
    // to monitor it ans debug it
    // Sinks generally have Future Jedi values
    // Some Flows offer some control mechanisms like KIll Switches that allow you to stop the stream at any point
    // Some Sources offer entrypoint through which you can send data inside the stream as you wish
    // Jedi/Materialized values are called in Akka = Materialized Values
    // Jedi/Materialized values may or may not be connected to the actual elements that go through the graph
    // Jedi/Materialized values can have ANY type
  }
}
