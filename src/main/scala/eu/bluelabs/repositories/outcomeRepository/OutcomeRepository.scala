package eu.bluelabs
package repositories

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

import entities._

object OutcomeRepository extends Repository[Outcome] {

  private def stringToOutcome(outcomeStr: String): Outcome = {
    val Array(id, fixtureName, outcomeName, _*) =
      outcomeStr.split(",").map(_.trim)
    Outcome(id, fixtureName, outcomeName)
  }

  override def getById(id: String): Future[Either[Throwable, Option[Outcome]]] =
    Future {
      try {
        val maybeOutcome = os
          .read
          .lines(os.resource / "db" / "outcomes.csv")
          .map(stringToOutcome)
          .filter(_.id == id)
          .headOption

        Right(maybeOutcome)
      }
      catch {
        case err: Throwable =>
          Left(err)
      }
    }

  override def getMany: Future[Either[Throwable, Set[Outcome]]] = ???

  override def createOne(
      item: Outcome
    ): Future[Either[Throwable, Outcome]] = ???

  override def updateById(
      id: String
    )(
      update: Outcome => Outcome
    ): Future[Either[Throwable, Outcome]] = ???

}
