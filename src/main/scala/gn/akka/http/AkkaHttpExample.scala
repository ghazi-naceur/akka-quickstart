package gn.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer

import java.net.URLEncoder
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

// https://www.youtube.com/watch?v=Agze0Ule5_0
object AkkaHttpExample {

  implicit val system: ActorSystem = ActorSystem() // Akka actors
  implicit val materializer: ActorMaterializer = ActorMaterializer() // Akka streams
  // => 'system' and 'materializer' are the foundation of writing a Akka HTTP code

  import system.dispatcher // Thread pool to spin up our http requests
  val source: String =
    """
      |object SimpleApp {
      |  val aField = 2
      |  def aMethod(x: Int) = x + 1
      |  def main(args: Array[String]): Unit = println(aField)
      |}
      |""".stripMargin

  val request: HttpRequest = HttpRequest(
    method = HttpMethods.POST,
    uri = "http://markup.su/api/highlighter",
    entity = HttpEntity(
      ContentTypes.`application/x-www-form-urlencoded`,
      s"source=${URLEncoder.encode(source, "UTF-8")}&language=Scala&theme=Sunburst"
    )
  )

  def sendRequest(): Future[String] = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
    val entityFuture: Future[HttpEntity.Strict] = responseFuture.flatMap(response => response.entity.toStrict(2.seconds))
    entityFuture map (entity => entity.data.utf8String)
  }

  def main(args: Array[String]): Unit = {
    sendRequest().foreach(println)
  }
}
