package eu.bluelabs
package services

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.actor.ActorSystem
import akka.stream.scaladsl._

import entities._
import eventSources.EventSourcesApi

class BetEventListenerService private (
    betService: BetServiceApi,
    accountService: AccountServiceApi,
    notificationService: NotificationServiceApi,
    outcomeService: OutcomeServiceApi
  ) extends BetEventListenerServiceApi {

  private var betPlacementReceived: Option[BetPlacementEvent => Unit]   = None
  private var betSettlementReceived: Option[BetSettlementEvent => Unit] = None
  private var betCreated: Option[Either[Throwable, Bet] => Unit]        = None
  private var betSettled: Option[Either[Throwable, Bet] => Unit]        = None
  private var betPlacementsDrained: Option[Option[Throwable] => Unit]   = None
  private var betSettlementsDrained: Option[Option[Throwable] => Unit]  = None

  private val placementToBet =
    Flow[BetPlacementEvent].map {
      case bet: BetPlaced =>
        val accAndOutcomeFuture = for {
          account <- accountService.getAccount(bet.accountId)
          outcome <- outcomeService.getOutcome(bet.outcomeId)
        } yield (account, outcome)

        accAndOutcomeFuture.map { accAndOutcome =>
          val (accountOrError, outcomeOrError) = accAndOutcome
          val betOrError: Either[ServiceError, Bet] =
            for {
              account <- accountOrError
              outcome <- outcomeOrError
            } yield Bet(
              bet.betId,
              account,
              outcome,
              BetStatus.Open,
              bet.payout,
              bet.timestamp
            )

          betOrError
        }

    }

  private val persistBet =
    Flow[Future[Either[ServiceError, Bet]]].map { futureBet =>
      futureBet.flatMap {
        case Left(err)  => Future.successful(Left(err))
        case Right(bet) => betService.saveBet(bet)
      }
    }

  private val updateBetStatus =
    Flow[BetSettlementEvent].map {
      case BetLost(betId, timestamp) =>
        betService.updateStatus(betId, BetStatus.Lost, timestamp)
      case BetWon(betId, timestamp) =>
        betService.updateStatus(betId, BetStatus.Won, timestamp)
    }

  private val sendNotification =
    Flow[Future[Either[BetServiceError, Bet]]].map {
      _.map {
        _.map { bet =>
          val message = s"""|
                            |${bet.account.name}, congratulations you just won
                            |${bet.payout} on ${bet.outcome.fixtureName},
                            |${bet.outcome.outcomeName}!
                            |""".stripMargin

          val dontNotifyFuture = Future.successful(Right {})

          val errorOrUnit: Future[Either[ServiceError, Unit]] =
            if (bet.status == BetStatus.Won) {
              betService.markWinnerAndGet(bet.id).flatMap {
                // if this returned false, that means that winner was already marked,
                // hence, notification already sent.
                case Right(didMarkTheWinner) =>
                  if (!didMarkTheWinner) dontNotifyFuture
                  else
                    notificationService
                      .send(
                        Notification(bet.account.phoneNumber, message)
                      )
                case Left(err) => Future.successful(Left(err))
              }
            }
            else dontNotifyFuture
          errorOrUnit
        }
      }.flatMap {
        case Left(err) => Future.successful(Left(err))
        case Right(f)  => f.map(Right(_))
      }.map {
        case Left(err) => Left(err)
        case Right(errOrUnit) =>
          errOrUnit match {
            case Left(err)   => Left(err)
            case Right(unit) => Right(unit)
          }
      }

    }

  private def intercept[T](
      maybeHandler: Option[T => Unit]
    ) =
    Flow[T].map { item =>
      maybeHandler.map(handler =>
        tryOrLog {
          handler(item)
        }
      )
      item
    }

  private val fireBetCreatedEvent =
    Flow[Future[Either[ServiceError, Bet]]].map(_.map { errOrBet =>
      betCreated.map { handler =>
        errOrBet match {
          case Left(err) =>
            err.throwable match {
              case None =>
                tryOrLog {
                  handler(Left(new Throwable(err.msg)))
                }
              case Some(throwable) =>
                tryOrLog {
                  handler(Left(throwable))
                }
            }
          case Right(bet) =>
            tryOrLog {
              handler(Right(bet))
            }
        }
      }
      errOrBet
    })

  private val fireBetSettledEvent =
    Flow[Future[Either[BetServiceError, Bet]]].map(_.map { errOrBet =>
      betSettled.map { handler =>
        errOrBet match {
          case Left(err) =>
            err.throwable match {
              case None =>
                tryOrLog {
                  handler(Left(new Throwable(err.msg)))
                }
              case Some(throwable) =>
                tryOrLog {
                  handler(Left(throwable))
                }
            }
          case Right(bet) =>
            tryOrLog {
              handler(Right(bet))
            }
        }
      }
      errOrBet
    })

  def startListeningTo(
      sources: EventSourcesApi
    )(implicit
      system: ActorSystem
    ): Unit = {
    sources
      .betPlacementEvents
      .via(intercept(betPlacementReceived))
      .via(placementToBet)
      .via(persistBet)
      .via(fireBetCreatedEvent)
      .runWith(Sink.seq)
      .flatMap(Future.sequence(_))
      .onComplete {
        case Failure(err) =>
          betPlacementsDrained.map(_.apply(Some(err)))
        case Success(_) =>
          betPlacementsDrained.map(_.apply(None))
          sources
            .betSettlementEvents
            .via(intercept(betSettlementReceived))
            .via(updateBetStatus)
            .via(fireBetSettledEvent)
            .via(sendNotification)
            .runWith(Sink.seq)
            .flatMap(Future.sequence(_))
            .onComplete {
              case Failure(err) =>
                betSettlementsDrained.map(_.apply(Some(err)))
              case Success(seq) =>
                betSettlementsDrained.map(_.apply(None))
                seq
                  .foreach {
                    case Left(err) => Log.error(err.msg)
                    case Right(_)  =>
                  }

            }
      }
  }

  override def onBetPlacementReceived(
      handler: BetPlacementEvent => Unit
    ): Unit = betPlacementReceived = Some(handler)

  override def onBetSettlementReceived(
      handler: BetSettlementEvent => Unit
    ): Unit =
    betSettlementReceived = Some(handler)

  def onBetCreated(handler: Either[Throwable, Bet] => Unit) = betCreated = Some(
    handler
  )
  def onBetSettled(handler: Either[Throwable, Bet] => Unit): Unit = betSettled =
    Some(handler)

  def onBetPlacementsDrained(handler: Option[Throwable] => Unit): Unit =
    betPlacementsDrained = Some(handler)
  def onBetSettlementsDrained(handler: Option[Throwable] => Unit): Unit =
    betSettlementsDrained = Some(handler)

}

object BetEventListenerService {

  def apply(
      betService: BetServiceApi,
      accountService: AccountServiceApi,
      notificationService: NotificationServiceApi,
      outcomeService: OutcomeServiceApi
    ): BetEventListenerServiceApi = new BetEventListenerService(
    betService,
    accountService,
    notificationService,
    outcomeService
  )
}
