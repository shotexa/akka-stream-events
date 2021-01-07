package eu.bluelabs
package services

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat

import entities._
import routes.BetRouter

class DeliveryService private (betService: BetServiceApi)
    extends DeliveryServiceApi {

  private val mainRouter: Route = concat(
    BetRouter(betService)
  )

  def connect(host: String, port: Int)(implicit system: ActorSystem) =
    Http().newServerAt(host, port).bind(mainRouter)

}
object DeliveryService {
  def apply(betService: BetServiceApi): DeliveryServiceApi =
    new DeliveryService(betService)
}
