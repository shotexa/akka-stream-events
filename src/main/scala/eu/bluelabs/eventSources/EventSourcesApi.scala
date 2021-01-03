package eu.bluelabs
package eventSources

import akka.NotUsed
import akka.stream.scaladsl._

import entities._


trait EventSourcesApi {
  def betPlacementEvents: Source[BetPlacementEvent, NotUsed]
  def betSettlementEvents: Source[BetSettlementEvent, NotUsed]
}
