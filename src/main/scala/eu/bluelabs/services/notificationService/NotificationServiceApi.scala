package eu.bluelabs
package services
import scala.concurrent.Future

import entities._

trait NotificationServiceApi {
  def send(
      notification: Notification
    ): Future[Either[NotificationServiceError, Unit]]
}
