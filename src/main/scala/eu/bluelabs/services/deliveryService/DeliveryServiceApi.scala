package eu.bluelabs
package services

import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.http.scaladsl.Http.ServerBinding

trait DeliveryServiceApi {
  def connect(
      host: String,
      port: Int
    )(implicit
      system: ActorSystem
    ): Future[ServerBinding]

}
