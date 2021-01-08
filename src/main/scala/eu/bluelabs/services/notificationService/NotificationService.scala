package eu.bluelabs
package services
package notificationService

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import entities._


class NotificationService private () extends NotificationServiceApi {
  override def send(
      notification: Notification
    ): Future[Either[NotificationServiceError, Unit]] = Future {
    try {

      /**
       * Wrapping println in try/catch does not makes much sense obviously, but I'd assume
       * that in "real world" NotificationService would send an SMS to a persons
       * phone, so I did it anyway, as a demonstration.
       */
      Right(println(notification.message))
    }
    catch {
      case e: Throwable => Left(NotificationServiceError(e.getMessage, Some(e)))
    }
  }

}

object NotificationService {
  def apply(): NotificationService = new NotificationService
}
