package eu.bluelabs

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.server.Route

object HttpServerRoutingMinimal {

  implicit val system = ActorSystem("my-system")

  // needed for the future flatMap/onComplete in the end
  val route: Route = ctx => ctx.complete("OK")

  println("Going to bind now")
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind())                 // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
