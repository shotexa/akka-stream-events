package eu.bluelabs

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.server.Route

object Main1 {

  val r = "abc".r

}
