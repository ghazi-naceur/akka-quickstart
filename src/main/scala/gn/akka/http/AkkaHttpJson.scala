package gn.akka.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import spray.json._

import java.util.UUID

case class Person(name: String, age: Int)
case class UserAdded(id: String, timestamp: Long)

trait PersonJsonProtocol extends DefaultJsonProtocol {
  // 'personFormat' is an automatic conversion between the Person case class and a Person internal JSON-like representation
  // we use 'jsonFormat2', because both 'Person' and 'UserAdded' have 2 fields
  implicit val personFormat: RootJsonFormat[Person] = jsonFormat2(Person)
  implicit val userAddedFormat: RootJsonFormat[UserAdded] = jsonFormat2(UserAdded)
}

// https://www.youtube.com/watch?v=yU85EowqhY4
object AkkaHttpJson extends PersonJsonProtocol with SprayJsonSupport {
  // extending PersonJsonProtocol in order to add the implicit conversion inside this scope
  // extending SprayJsonSupport in order to add some additional implicits to turning that Json-like internal representation
  // of Person or UserAdded into the requests and responses that Akka Http can understand
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "AkkaHttpJson")

  val akkaHttpRoute: Route = (path("akka" / "user") & post) {
    complete("received!")
  }

  val sprayRoute: Route = (path("spray" / "user") & post) {
    // 'as' will fetch whatever converter you have for the specified type (in our case: Person)
    entity(as[Person]) { person: Person =>
      complete(UserAdded(UUID.randomUUID().toString, System.currentTimeMillis()))
    }
  }

  def main(args: Array[String]): Unit = {
    Http()
      .newServerAt("localhost", 9050)
      //      .bind(akkaHttpRoute)
      .bind(sprayRoute)

  }
}
