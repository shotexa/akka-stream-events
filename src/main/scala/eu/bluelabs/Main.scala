package eu.bluelabs

import akka.actor.ActorSystem

import services._
import repositories._
import entities._
import eventSources.EventSources
object Main extends App {

  implicit val system: ActorSystem = ActorSystem("bet-history")

  val accountService: AccountServiceApi           = AccountService(AccountRepository)
  val betService: BetServiceApi                   = BetService(BetRepository)
  val outcomeService: OutcomeServiceApi           = OutcomeService(OutcomeRepository)
  val notificationService: NotificationServiceApi = NotificationService()

  val betEventListenerService: BetEventListenerServiceApi =
    BetEventListenerService(
      betService,
      accountService,
      notificationService,
      outcomeService
    )

  betEventListenerService.onBetSettlementReceived {
    case BetLost(betId, timestamp) =>
      Log.info(s"Received bet lost event for bet -> $betId")
    case BetWon(betId, timestamp) =>
      Log.info(s"Received bet won event for bet -> $betId")
  }
  betEventListenerService.onBetPlacementReceived {
    case event @ BetPlaced(betId, accountId, outcomeId, payout, timestamp) =>
      Log.info(s"Received bet placement event: $event")
  }

  betEventListenerService.onBetCreated {
    case Left(err)  => Log.error(err.getMessage)
    case Right(bet) => Log.success(s"Created bet ${bet.id}")
  }

  betEventListenerService.onBetSettled {
    case Left(err) => Log.error(err.getMessage)
    case Right(bet) =>
      Log.success(s"Settled the bet ${bet.id} with status ${bet.status}")
  }

  betEventListenerService.onBetPlacementsDrained {
    case None      => Log.success("Finished placing bets")
    case Some(err) => Log.error(err.getMessage)
  }

  betEventListenerService.onBetSettlementsDrained { maybeError =>
    maybeError match {
      case None      => Log.success("Finished bet settlements")
      case Some(err) => Log.error(err.getMessage)
    }
    system.terminate()
  }

  betEventListenerService.startListeningTo(EventSources)

}
