package eu.bluelabs
package services
package outcomeService

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import repositories.Repository
import entities._

class OutcomeService private (db: Repository[Outcome])
    extends OutcomeServiceApi {

  override def getOutcome(
      outcomeId: String
    ): Future[Either[OutcomeServiceError, Outcome]] = db
    .getById(outcomeId)
    .map {
      case Left(err) => Left(OutcomeServiceError(err.getMessage, Some(err)))
      case Right(outcome) =>
        outcome match {
          case None =>
            Left(
              OutcomeServiceError(
                s"Outcome with an id of $outcomeId not found",
                None
              )
            )
          case Some(out) => Right(out)
        }
    }

}

object OutcomeService {
  def apply(db: Repository[Outcome]): OutcomeService = new OutcomeService(db)
}
