package eu.bluelabs
package services
package deliveryService.responseEntities

import spray.json.DefaultJsonProtocol._
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat

import entities._

object BetResponseEntities {
  object Entities {
    final case class BetEntity(
        betId: String,
        payout: Float,
        status: BetStatus,
        fixtureName: String,
        outcomeName: String,
        cursor: String
      )

    final case class Navigation(
        hasNextPage: Boolean,
        hasPreviousPage: Boolean,
        firstCursor: String,
        lastCursor: String
      )

    final case class GetBetsResponseEntity(
        nodes: Seq[BetEntity],
        navigation: Navigation
      )
  }
  object JsonFormatImplicits {
    import Entities._
    
    implicit val betStatusJsonFormat: RootJsonFormat[BetStatus] =
      new RootJsonFormat[BetStatus] {
        def read(json: JsValue): BetStatus = json match {
          case JsString(strStatus) =>
            if (strStatus == BetStatus.Won.toString) BetStatus.Won
            else if (strStatus == BetStatus.Lost.toString) BetStatus.Lost
            else BetStatus.Open
          case _: JsValue => BetStatus.Open
        }
        def write(obj: BetStatus): JsValue = JsString(obj.toString)
      }

    implicit val betEntityJsonFormat: RootJsonFormat[BetEntity] =
      jsonFormat6(BetEntity)
    implicit val navigationJsonFormat: RootJsonFormat[Navigation] =
      jsonFormat4(Navigation)

    implicit val betsResponseEntityJsonFormat
        : RootJsonFormat[GetBetsResponseEntity] = jsonFormat2(
      GetBetsResponseEntity
    )
  }
}
