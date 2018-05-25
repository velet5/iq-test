import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}


object Server {

  final case class User(userId: String)

  // collect your json format instances into a support trait:
  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val itemFormat: RootJsonFormat[Server.User] = jsonFormat1(Server.User)
  }

  private val log: Logger = Logger(getClass)
  
}


class Server(port: Port, counter: Counter) extends Directives with Server.JsonSupport {

  import Server._

  private implicit val system: ActorSystem = ActorSystem("my-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val route: Route =
    path("stats") {
      get(complete(counter.size().map(_.toString)))
    } ~
    path("user") {
      post {
        entity(as[User]) { user =>
          counter.count(user.userId)
          complete("")
        }
      }
    }

  def start(): RunningServer = {
    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(route, "localhost", port.value)

    log.info(s"Started HTTP server at port ${port.value}")

    new RunningServer(system, bindingFuture)
  }

}


object RunningServer {

  private val log = Logger(getClass)

}


class RunningServer(system: ActorSystem, bindingFuture: Future[Http.ServerBinding]) {

  import RunningServer._
  
  private implicit val executionContext: ExecutionContext = system.dispatcher

  def stop(): Unit = {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => {
      system.terminate()
      log.info("Server port unbinded, actor system terminated")
    })
  }

}
