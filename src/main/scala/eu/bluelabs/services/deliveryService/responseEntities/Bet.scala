package eu.bluelabs
package services
package responseEntities

import entities._

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
