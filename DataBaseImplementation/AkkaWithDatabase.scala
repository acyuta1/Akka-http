import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.io.StdIn
import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import scala.util.Random
import scala.collection.mutable

object AkkaWithDatabase {


  final case class fetch1(param:String)

  val sql1 = "insert into finalurl2 set urlt=?,count=? on duplicate key update count=?"
  val url = "jdbc:mysql://127.0.0.1/url?useServerPrepStmts=false&rewriteBatchedStatements=true"
  val username = "root"
  val password = "Password8$"

  Class.forName("com.mysql.jdbc.Driver")
  var connection = DriverManager.getConnection(url, username, password)

  var hm: mutable.HashMap[String,Int] = new mutable.HashMap()

  private var count:Int=0
  val stm: PreparedStatement = connection.prepareStatement(sql1)
  val stm2 =  connection.createStatement()

  def insertIntoDB(path: String) = {
    if(hm.keySet.exists(_ == path)) {
      hm(path)+= (math.random * (1000-1) + 1).toInt
      stm.setString(1,path)
      stm.setInt(2,hm(path))
      stm.setInt(3,hm(path))
      stm.addBatch()
    } else {
      hm.put(path,1)
      stm.setString(1,path)
      stm.setInt(2,hm(path))
      stm.setInt(3,hm(path))
      stm.execute()
    }
    if(count%10000==0){
      stm.executeBatch()
    }
  }

  class ActorA extends Actor with ActorLogging {
    def receive = {
      case fetch1(param) =>
        insertIntoDB(param)
        count+=1
    }
  }

  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout: Timeout = 1.seconds

    val hr = Array (1,2,3,4,5,6,7,8,9,10)

    val actor1 = system.actorOf(Props[ActorA], "SimpleActor1")

    val route =
      concat(
      path("path1") {

        val rand = new Random(System.currentTimeMillis())
        val random_index2 = rand.nextInt(hr.length)
        val hour = hr(random_index2)
        val finalresult = "path1-"+hour

        actor1 ! fetch1(finalresult)
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Path1: Hello</h1> <"))
      },
        path("path2") {

          val rand = new Random(System.currentTimeMillis())
          val random_index2 = rand.nextInt(hr.length)
          val hour = hr(random_index2)
          val finalresult = "path2-"+hour

          actor1 ! fetch1(finalresult)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Path2: Hello</h1> <"))
        },
        path("path3") {

          val rand = new Random(System.currentTimeMillis())
          val random_index2 = rand.nextInt(hr.length)
          val hour = hr(random_index2)
          val finalresult = "path3-"+hour

          actor1 ! fetch1(finalresult)
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Path3: Hello</h1> <"))
        }
      )


    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    val _ = bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
    connection.close()
  }
}
