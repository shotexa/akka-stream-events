package eu.bluelabs
package services
package notificationService

import scala.concurrent.Future

import entities._

trait NotificationServiceApi {
  def send(
      notification: Notification
    ): Future[Either[NotificationServiceError, Unit]]
}
