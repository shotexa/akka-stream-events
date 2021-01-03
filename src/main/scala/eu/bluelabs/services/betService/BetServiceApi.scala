package eu.bluelabs
package services

import java.time.Instant

import scala.concurrent.Future

import entities._

trait BetServiceApi {
  def saveBet(
      bet: Bet
    ): Future[Either[BetServiceError, Bet]]

  def getBet(betId: String): Future[Either[BetServiceError, Bet]]

  def getAllBets: Future[Either[BetServiceError, Set[Bet]]]

  def updateStatus(
      betId: String,
      status: BetStatus,
      timestamp: Instant
    ): Future[Either[BetServiceError, Bet]]

  def markWinnerAndGet(betId: String): Future[Either[BetServiceError, Boolean]]
}
