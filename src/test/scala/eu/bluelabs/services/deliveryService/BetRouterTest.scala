package eu.bluelabs
package test
package services

import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import eu.bluelabs.repositories.BetRepository
import eu.bluelabs.services.betService.BetService
import eu.bluelabs.services.betService.BetServiceApi
import eu.bluelabs.services.deliveryService.responseEntities.BetResponseEntities.Entities._
import eu.bluelabs.services.deliveryService.responseEntities.BetResponseEntities.JsonFormatImplicits._
import eu.bluelabs.services.deliveryService.routes.BetRouter
import org.scalatest.Suite

import entities._

trait BetRouterTest
    extends TestSuite with ScalatestRouteTest with SprayJsonSupport {
  this: Suite =>

  val betService: BetServiceApi = BetService(BetRepository)
  val betRouter: Route          = BetRouter(betService)
  val testAccount = Account(
    id = "acc-0001",
    name = "Shota Jolbordi",
    phoneNumber = "123456"
  )
  val testOutcome = Outcome(
    id = "out-0001",
    fixtureName = "Team A vs Team B",
    outcomeName = "Nobody has won, it is a test"
  )
  val testBet = Bet(
    id = "bet-0001",
    account = testAccount,
    outcome = testOutcome,
    status = BetStatus.Open,
    payout = 0.0f,
    timestamp = Instant.now()
  )

}

class BetRouterTestSingleBetTest extends BetRouterTest {

  override protected def beforeAll(): Unit = {
    Await.result(BetRepository.truncate, 5.seconds)
    Await.result(betService.saveBet(testBet), 5.seconds)
  }

  describe("When there is only one bet in DB") {

    it("Should return 1 bet") {
      Get("/bets/acc-0001") ~> betRouter ~> check {
        val response = Await.result(
          Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
          5.seconds
        )
        response.nodes.length shouldBe 1
      }
    }

    it("hasNextPage and hasPreviousPage both should be false") {
      Get("/bets/acc-0001") ~> betRouter ~> check {
        val response = Await.result(
          Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
          5.seconds
        )
        response.navigation.hasNextPage shouldBe false
        response.navigation.hasPreviousPage shouldBe false
      }
    }

    it("should return a bet with an id bet-0001") {
      Get("/bets/acc-0001") ~> betRouter ~> check {
        val response = Await.result(
          Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
          5.seconds
        )
        val first = response.nodes.head
        first.betId shouldBe "bet-0001"
      }
    }

    describe("And I'm requesting a bet for an account that does not exists") {
      it("Should not find any bets") {
        Get("/bets/acc-0002") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )
          response.nodes.size shouldBe 0
        }
      }
    }

    describe("And I'm passing invalid argument to first") {
      it("Should return 400 Bed request error") {
        Get("/bets/acc-0001?first=blablabla") ~> Route.seal(
          betRouter
        ) ~> check {
          response.status shouldBe StatusCodes.BadRequest
        }
      }
    }

  }
}

class BetRouterMultiplyBetsTest extends BetRouterTest {

  override protected def beforeAll(): Unit = {
    Await.result(BetRepository.truncate, 5.seconds)

    val future = for {
      _ <- betService.saveBet(testBet)
      _ <- betService.saveBet(testBet.copy(id = "bet-0002"))
      _ <- betService.saveBet(testBet.copy(id = "bet-0003"))
    } yield ()

    Await.result(future, 5.seconds)
  }

  describe("When there is are 3 bets in DB") {

    it("Should return 3 bets") {
      Get("/bets/acc-0001") ~> betRouter ~> check {
        val response = Await.result(
          Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
          5.seconds
        )
        response.nodes.length shouldBe 3
      }
    }

    describe("And I'm getting first 2 bets") {

      it("Should only find 2 bets") {
        Get("/bets/acc-0001?first=2") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )
          response.nodes.length shouldBe 2
        }
      }

      it("hasNextPage should be true and hasPreviousPage should be false") {
        Get("/bets/acc-0001?first=2") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )
          response.navigation.hasNextPage shouldBe true
          response.navigation.hasPreviousPage shouldBe false
        }
      }
    }

    describe("And I'm getting bets after bet-0001") {
      it("Should return 2 bets, with ids bet-0002 and bet-0003") {
        Get("/bets/acc-0001?after=bet-0001") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.nodes.length shouldBe 2
          response.nodes(0).betId shouldBe "bet-0002"
          response.nodes(1).betId shouldBe "bet-0003"
        }
      }

      it("hasPreviousPage should be true and hasNextPage should be false") {
        Get("/bets/acc-0001?after=bet-0001") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.navigation.hasNextPage shouldBe false
          response.navigation.hasPreviousPage shouldBe true
        }
      }

    }

    describe("And I'm getting bets before bet-0001") {
      it("Should not find any bets") {
        Get("/bets/acc-0001?before=bet-0001") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.nodes.length shouldBe 0
        }
      }

      it("hasPreviousPage and hasNextPage should be false") {
        Get("/bets/acc-0001?before=bet-0001") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.navigation.hasNextPage shouldBe false
          response.navigation.hasPreviousPage shouldBe false
        }
      }

    }

    describe("And I'm getting bets before bet-0003") {
      it("Should return 2 bets, with ids bet-0001 and bet-0002") {
        Get("/bets/acc-0001?before=bet-0003") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.nodes.length shouldBe 2
          response.nodes(0).betId shouldBe "bet-0001"
          response.nodes(1).betId shouldBe "bet-0002"
        }
      }
    }

    describe("And I'm getting bets after bet-0003") {
      it("Should not find any bets") {
        Get("/bets/acc-0001?after=bet-0003") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.nodes.length shouldBe 0

        }
      }
      it("hasPreviousPage and hasNextPage should be false") {
        Get("/bets/acc-0001?after=bet-0003") ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.navigation.hasNextPage shouldBe false
          response.navigation.hasPreviousPage shouldBe false
        }
      }
    }

    describe("I'm using both before and after query parameters") {
      it("Should only consider before") {
        Get(
          "/bets/acc-0001?before=bet-0003&after=bet-0001"
        ) ~> betRouter ~> check {
          val response = Await.result(
            Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
            5.seconds
          )

          response.nodes.length shouldBe 2
          response.nodes(0).betId shouldBe "bet-0001"
          response.nodes(1).betId shouldBe "bet-0002"
        }
      }
    }

    describe("if either before or after query parameters are invalid") {

      describe("in case of before") {
        it("Should ignore it") {
          Get(
            "/bets/acc-0001?before=blablabla"
          ) ~> betRouter ~> check {
            val response = Await.result(
              Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
              5.seconds
            )
            response.nodes.length shouldBe 3
          }
        }
      }

      describe("in case of after") {
        it("Should ignore it") {
          Get(
            "/bets/acc-0001?after=blablabla"
          ) ~> betRouter ~> check {
            val response = Await.result(
              Unmarshal(responseAs[String]).to[GetBetsResponseEntity],
              5.seconds
            )
            response.nodes.length shouldBe 3
          }
        }
      }

    }

  }
}
