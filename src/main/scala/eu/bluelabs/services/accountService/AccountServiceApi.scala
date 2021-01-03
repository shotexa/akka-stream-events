package eu.bluelabs
package services

import scala.concurrent.Future
import entities._

trait AccountServiceApi {
  def getAccount(
      accountId: String
    ): Future[Either[AccountServiceError, Account]]
}
