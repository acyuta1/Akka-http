import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import scala.io.StdIn

object HitCounterImpFinal {


  final case object fetch

  class ActorClass extends Actor with ActorLogging {
    
    private val number = new AtomicInteger() // thread-safe programming on single variables
    def receive = {
      case fetch =>
        number.incrementAndGet()
        context.sender() ! number
    }
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout: Timeout = 2.seconds

    val actor1 = system.actorOf(Props[ActorClass], "SimpleActor")
    val route =
      path("counter") {
        get {
          onComplete((actor1 ? fetch).mapTo[Int])
          {
            number => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>You visited $number times</h1>"))

          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    val _ = bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
  }
}
