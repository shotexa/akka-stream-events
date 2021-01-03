package eu.bluelabs
package entities

import java.time.Instant

trait ServiceError {
  val msg: String
  val throwable: Option[Throwable]
}

sealed trait BetPlacementEvent

final case class BetPlaced(
    betId: String,
    accountId: String,
    outcomeId: String,
    payout: Float,
    timestamp: Instant
  ) extends BetPlacementEvent

sealed trait BetSettlementEvent

sealed trait BetStatus
object BetStatus {
  case object Won  extends BetStatus
  case object Lost extends BetStatus
  case object Open extends BetStatus
}

final case class Notification(phoneNumber: String, message: String)

final case class BetWon(betId: String, timestamp: Instant)
    extends BetSettlementEvent

final case class BetLost(betId: String, timestamp: Instant)
    extends BetSettlementEvent

final case class Outcome(id: String, fixtureName: String, outcomeName: String)

final case class Account(id: String, name: String, phoneNumber: String)

final case class Bet(
    id: String,
    account: Account,
    outcome: Outcome,
    status: BetStatus,
    payout: Float,
    timestamp: Instant
  )

final case class OutcomeServiceError(msg: String, throwable: Option[Throwable])
    extends ServiceError

final case class AccountServiceError(msg: String, throwable: Option[Throwable])
    extends ServiceError

final case class BetServiceError(msg: String, throwable: Option[Throwable])
    extends ServiceError

final case class NotificationServiceError(msg: String, throwable: Option[Throwable])
    extends ServiceError
