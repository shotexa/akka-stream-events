package eu.bluelabs
package services
package accountService

import scala.concurrent.Future
import entities._

trait AccountServiceApi {
  def getAccount(
      accountId: String
    ): Future[Either[AccountServiceError, Account]]
}
