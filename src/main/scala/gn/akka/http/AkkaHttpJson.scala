package gn.akka.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import spray.json._

// https://www.youtube.com/watch?v=yU85EowqhY4
object AkkaHttpJson {

  case class Person(name: String, age: Int)
  case class UserAdded(id: String, timestamp: Long)

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "AkkaHttpJson")

  val route: Route = (path("api" / "user") & post) {
    complete("received!")
  }

  def main(args: Array[String]): Unit = {
    Http().newServerAt("localhost", 9050).bind(route)
  }
}
