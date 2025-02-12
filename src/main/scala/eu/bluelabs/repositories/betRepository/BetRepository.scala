package eu.bluelabs
package repositories

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import entities._

object BetRepository extends Repository[Bet] {

  /**
   *  Using AtomicReference here because bets is a shared resource among many threads
   */
  private val bets: AtomicReference[Map[String, Bet]] = new AtomicReference(
    Map.empty
  )

  override def getById(id: String): Future[Either[Throwable, Option[Bet]]] =
    Future {
      try {
        Right(bets.get.get(id))
      }
      catch {
        case err: Throwable => Left(err)
      }
    }

  override def getMany: Future[Either[Throwable, Set[Bet]]] = Future {
    Right(bets.get.values.toSet)
  }

  override def createOne(
      item: Bet
    ): Future[Either[Throwable, Bet]] = Future {
    try {
      if (bets.get.contains(item.id))
        Left(new Exception(s"Bet with an id of ${item.id} already exists"))
      else {
        bets.updateAndGet(curr => curr.updated(item.id, item))
        Right(bets.get.apply(item.id))
      }
    }
    catch {
      case err: Throwable => Left(err)
    }
  }

  override def updateById(
      id: String
    )(
      update: Bet => Bet
    ): Future[Either[Throwable, Bet]] = Future {
    try {
      if (!bets.get.contains(id))
        Left(new Exception(s"Bet with an id of ${id} does not exists"))
      else {

        /**
         * atomically update and get the bet
         */
        bets.updateAndGet { curr =>
          val bet = curr(id)
          curr.updated(id, update(bet))
        }
        Right(bets.get.apply(id))
      }
    }
    catch {
      case err: Throwable => Left(err)
    }
  }

  def truncate: Future[Either[Throwable, Unit]] = Future {
    try {
      bets.updateAndGet(curr => Map.empty[String, Bet])
      Right {}
    }
    catch {
      case err: Throwable => Left(err)
    }
  }
}
