package eu.bluelabs
package services
package deliveryService

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import betService._
import entities._
import routes.BetRouter

class DeliveryService private (betService: BetServiceApi)
    extends DeliveryServiceApi {

  private val mainRouter: Route = concat(
    BetRouter(betService)
    //... some other routes
  )

  def connect(host: String, port: Int)(implicit system: ActorSystem) =
    Http().newServerAt(host, port).bind(mainRouter)

}
object DeliveryService {
  def apply(betService: BetServiceApi): DeliveryServiceApi =
    new DeliveryService(betService)
}
