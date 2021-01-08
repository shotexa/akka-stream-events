package eu.bluelabs
package services
package deliveryService
package routes

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.http.javadsl.model.StatusCode
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import responseEntities.BetResponseEntities.Entities._
import responseEntities.BetResponseEntities.JsonFormatImplicits._
import entities._
import betService._

class BetRouter private (betService: BetServiceApi)
    extends Function[Unit, Route] with SprayJsonSupport {

  private def numericBetId(str: String): Int = str.split("-").last.toInt

  private def getByAccountId = path("bets" / Segment) { accId =>
    get {
      parameters(
        "first".as[Int].withDefault(10),
        "before".optional,
        "after".optional
      ) { (first, maybeBefore, maybeAfter) =>
        val regex = "bet-\\d+".r
        val before = maybeBefore match {
          case x @ Some(str) => if (regex.matches(str)) x else None
          case None          => None
        }
        val after = maybeAfter match {
          case x @ Some(str) => if (regex.matches(str)) x else None
          case None          => None
        }
        onComplete {
          betService
            .getAllBets
            .map(_.map { set =>
              val sorted =
                set
                  .filter(_.account.id == accId)
                  .toSeq
                  .sortBy(_.id) { (a, b) =>
                    numericBetId(a) - numericBetId(b)
                  }

              val bets =
                sorted
                  .filter { bet: Bet =>
                    if (before.nonEmpty)
                      numericBetId(bet.id) < numericBetId(before.get)
                    else if (after.nonEmpty)
                      numericBetId(bet.id) > numericBetId(after.get)
                    else true
                  }
                  .take(first)

              (
                numericBetId(bets.head.id) > numericBetId(sorted.head.id),
                bets,
                numericBetId(bets.last.id) < numericBetId(sorted.last.id)
              )
            })
        } {
          case Failure(err) => complete(InternalServerError, err.getMessage)
          case Success(betsOrErr) =>
            betsOrErr match {
              case Left(err) =>
                complete(
                  InternalServerError,
                  err.msg
                )
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
