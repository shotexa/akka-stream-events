package eu.bluelabs

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Failure
import scala.util.Success

import akka.actor.ActorSystem

import services.accountService._
import services.betService._
import services.outcomeService._
import services.notificationService._
import services.deliveryService._
import services.betEventListenerService._
import repositories._
import entities._
import eventSources.EventSources

object Main extends App {

  val betEventListenerActorSystem: ActorSystem = ActorSystem(
    "bet-event-listener"
  )
  val httpServerActorSystem: ActorSystem = ActorSystem("http-server")

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

  val deliveryService: DeliveryServiceApi = DeliveryService(betService)

  betEventListenerService.onBetSettlementReceived {
    case BetLost(betId, timestamp) =>
      Log.info(s"Received bet lost event for bet -> $betId")
    case BetWon(betId, timestamp) =>
      Log.info(s"Received bet won event for bet -> $betId")
  }

  betEventListenerService.onBetPlacementReceived {
    case event @ BetPlaced(_, _, _, _, _) =>
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

  betEventListenerService.onBetSettlementsDrained {
    case None      => Log.success("Finished bet settlements")
    case Some(err) => Log.error(err.getMessage)

  }

  betEventListenerService.startListeningTo(EventSources)(
    betEventListenerActorSystem
  )

  deliveryService
    .connect("0.0.0.0", 8080)(
      httpServerActorSystem
    )
    .onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        Log.info(
          s"Server started at http://${address.getHostString}:${address.getPort}"
        )
        Log.info("Press ENTER to terminate")
        io.StdIn.readLine()
        binding.unbind().onComplete { _ =>
          betEventListenerActorSystem.terminate()
          httpServerActorSystem.terminate()
        }

      case Failure(err) =>
        Log.error(err.getMessage)
        betEventListenerActorSystem.terminate()
        httpServerActorSystem.terminate()

    }
}
