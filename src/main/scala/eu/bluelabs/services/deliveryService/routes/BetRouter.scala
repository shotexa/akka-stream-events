package eu.bluelabs
package services
package routes

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import eu.bluelabs.repositories.BetRepository
import spray.json.DefaultJsonProtocol._
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat

import responseEntities._
import entities._

class BetRouter private (betService: BetServiceApi)
    extends Function[Unit, Route] with SprayJsonSupport {

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

  private def numericBetId(str: String): Int = str.split("-").last.toInt

  private def getByAccountId = path("bets" / Segment) { accId =>
    get {
      parameters("first".as[Int].withDefault(10)) { first =>
        concat(
          parameters("before".optional, "after".optional) { (before, after) =>
            def filterer(
                prevPage: Option[Bet],
                nextPage: Option[Bet],
                size: Int,
                takenSoFar: Int
              ): Bet => (Option[Bet], Boolean, Option[Bet]) = bet =>
              if (before.nonEmpty && after.nonEmpty) {
                val result = numericBetId(bet.id) < numericBetId(before.get)

                (prevPage, result, if (result) nextPage else Some(bet))
              }
              else if (before.nonEmpty && after.isEmpty) {
                val result = numericBetId(bet.id) < numericBetId(before.get)

                (prevPage, result, if (result) nextPage else Some(bet))
              }
              else if (before.isEmpty && after.nonEmpty) {
                val result = numericBetId(bet.id) > numericBetId(after.get)

                (if (result) prevPage else Some(bet), result, nextPage)
              }
              else 
                  (prevPage, true, nextPage)

            //   bet => numericBetId(bet.id) < numericBetId(before.get)
            // else if (before.isEmpty && after.nonEmpty)
            //   bet => numericBetId(bet.id) > numericBetId(after.get)
            // else _ => true

            onComplete {
              betService
                .getAllBets
                .map(_.map { set =>
                  val size = set.size

                  val (l, bets, r, _) =
                    set
                      // .filter(_.account.id == accId)
                      .toSeq
                      .sortBy(_.id) { (a, b) =>
                        numericBetId(a) - numericBetId(b)
                      }
                      .foldLeft(
                        (
                          Option.empty[Bet],
                          Vector.empty[Bet],
                          Option.empty[Bet],
                          0
                        )
                      ) {
                        case ((before, acc, after, takenSoFar), v) =>
                          if (takenSoFar < first) {
                            val (prevPage, bool, nextPage) =
                              filterer(before, after, size, takenSoFar)(v)

                            if (bool)
                              (prevPage, acc :+ v, nextPage, takenSoFar + 1)
                            else
                              (prevPage, acc, nextPage, takenSoFar)
                          }
                          else {
                            (before, acc, after, takenSoFar)
                          }
                      }

                  (l.nonEmpty, bets, r.nonEmpty)
                })
            } {
              case Failure(err) => complete(err.getMessage)
              case Success(betsOrErr) =>
                betsOrErr match {
                  case Left(err) => complete(err.msg)
                  case Right((hasPreviousPage, bets, hasNextPage)) =>
                    val response = GetBetsResponseEntity(
                      nodes = bets.map(bet =>
                        BetEntity(
                          betId = bet.id,
                          payout = bet.payout,
                          status = bet.status,
                          fixtureName = bet.outcome.fixtureName,
                          outcomeName = bet.outcome.outcomeName,
                          cursor = bet.id
                        )
                      ),
                      navigation = Navigation(
                        hasNextPage = hasNextPage,
                        hasPreviousPage = hasPreviousPage,
                        firstCursor = bets.headOption.map(_.id).getOrElse(""),
                        lastCursor = bets.lastOption.map(_.id).getOrElse("")
                      )
                    )
                    complete(response)
                }
            }
          }
        )
      }
    }
  }

  private def ping = path("ping") {
    get {
      complete("OK")
    }
  }

  override def apply(v1: Unit): Route = concat(
    getByAccountId,
    ping
  )

}
object BetRouter {

  def apply(betService: BetServiceApi): Route = new BetRouter(betService)(())

}
