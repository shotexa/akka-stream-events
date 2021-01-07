package eu.bluelabs
package services

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import repositories.Repository

import entities._

class BetService private (db: Repository[Bet]) extends BetServiceApi {

  // cache of winner bet ids that have been notified already 
  private val winnerNotifiedCache: AtomicReference[Set[String]] =
    new AtomicReference(Set.empty)

  override def markWinnerAndGet(
      betId: String
    ): Future[Either[BetServiceError, Boolean]] = Future {
    try {
      // if bet winner was already been notified, don't update cache, just return false
      // otherwise, update the cache and return true

      val prevValue = winnerNotifiedCache.getAndUpdate(curr => curr + betId)
      Right(!prevValue.contains(betId))

    }
    catch {
      case err: Throwable => Left(BetServiceError(err.getMessage, Some(err)))
    }
  }

  override def getAllBets: Future[Either[BetServiceError, Set[Bet]]] =
    db.getMany.map {
      case Left(err)   => Left(BetServiceError(err.getMessage, Some(err)))
      case Right(bets) => Right(bets)
    }

  override def getBet(betId: String): Future[Either[BetServiceError, Bet]] = {
    db.getById(betId).map {
      case Left(err) => Left(BetServiceError(err.getMessage, Some(err)))
      case Right(bet) =>
        bet match {
          case None =>
            Left(BetServiceError(s"Bet with an id of $betId not found", None))
          case Some(bet) => Right(bet)
        }
    }

  }

  override def saveBet(bet: Bet): Future[Either[BetServiceError, Bet]] =
    db.createOne(bet).map {
      case Left(err)  => Left(BetServiceError(err.getMessage, Some(err)))
      case Right(bet) => Right(bet)
    }

  def updateStatus(
      betId: String,
      status: BetStatus,
      timestamp: Instant
    ): Future[Either[BetServiceError, Bet]] = {
    db.updateById(betId) { bet =>
      bet.status match {
        case BetStatus.Open =>
          bet.copy(status = status, timestamp = timestamp)
        case BetStatus.Lost | BetStatus.Won =>
          if (bet.timestamp.isBefore(timestamp))
            bet.copy(status = status, timestamp = timestamp)
          else bet
      }
    }.map {
      case Left(err)  => Left(BetServiceError(err.getMessage, Some(err)))
      case Right(bet) => Right(bet)
    }

  }

}

object BetService {
  def apply(db: Repository[Bet]): BetService = new BetService(db)
}
