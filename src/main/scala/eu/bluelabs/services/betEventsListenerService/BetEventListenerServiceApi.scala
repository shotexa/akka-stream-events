package eu.bluelabs
package services

import eventSources.EventSourcesApi
import entities._
import akka.actor.ActorSystem

trait BetEventListenerServiceApi {
  def startListeningTo(
      sources: EventSourcesApi
    )(implicit
      system: ActorSystem
    ): Unit

  def onBetPlacementReceived(handler: BetPlacementEvent => Unit): Unit
  def onBetSettlementReceived(handler: BetSettlementEvent => Unit): Unit

  def onBetPlacementsDrained(handler: Option[Throwable] => Unit): Unit
  def onBetSettlementsDrained(handler: Option[Throwable] => Unit): Unit

  def onBetCreated(handler: Either[Throwable, Bet] => Unit): Unit
  def onBetSettled(handler: Either[Throwable, Bet] => Unit): Unit
}
