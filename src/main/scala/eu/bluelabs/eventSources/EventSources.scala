package eu.bluelabs
package eventSources

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import scala.concurrent.duration._

import akka.NotUsed
import akka.stream.scaladsl._

import entities._

object EventSources extends EventSourcesApi {

  private def parseDateString(dateString: String): Instant = {

    val formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneOffset.UTC)

    Instant.from(formatter.parse(dateString))
  }

  private val lineToPlacement: Flow[String, BetPlacementEvent, NotUsed] =
    Flow[String].map { line: String =>
      val Array(betId, accountId, outcomeId, payout, timestamp, _*) =
        line.split(",").map(_.trim)

      val instant = parseDateString(timestamp)

      BetPlaced(betId, accountId, outcomeId, payout.toFloat, instant)
    }

  private val lineToSettlement: Flow[String, BetSettlementEvent, NotUsed] =
    Flow[String].map { line: String =>
      val Array(bidId, result, timestamp, _*) = line.split(",").map(_.trim)

      val instant = parseDateString(timestamp)

      if (result == "won") BetWon(bidId, instant)
      else BetLost(bidId, instant)
    }

  override def betPlacementEvents: Source[BetPlacementEvent, NotUsed] =
    StreamConverters
      .fromInputStream { () =>
        os.read.inputStream(os.resource / "db" / "bet-placement-events.csv")
      }
      .via(Util.splitByNewLine)
      .map(_.utf8String)
      .drop(1)
      .via(Util.randomize)
      .via(Util.delayForEachBetween(1, 2000))
      .viaMat(lineToPlacement)(Keep.right)

  override def betSettlementEvents: Source[BetSettlementEvent, NotUsed] =
    StreamConverters
      .fromInputStream { () =>
        os.read.inputStream(os.resource / "db" / "bet-settlement-events.csv")
      }
      .via(Util.splitByNewLine)
      .map(_.utf8String)
      .drop(1)
      .via(Util.randomize)
      .via(Util.delayForEachBetween(1, 2000))
      .viaMat(lineToSettlement)(Keep.right)
}
