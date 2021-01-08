package eu.bluelabs
package services
package outcomeService

import scala.concurrent.Future
import entities._

trait OutcomeServiceApi {
  def getOutcome(
      outcomeId: String
    ): Future[Either[OutcomeServiceError, Outcome]]
}
