package eu.bluelabs
package repositories

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import entities._

object AccountRepository extends Repository[Account] {

  private def stringToAccount(accString: String): Account = {
    val Array(id, name, phoneNumber, _*) = accString.split(",").map(_.trim)
    Account(id, name, phoneNumber)
  }

  override def getById(
      id: String
    ): Future[Either[Throwable, Option[Account]]] = Future {

    try {
      val maybeAccount: Option[Account] = os
        .read
        .lines(os.resource / "db" / "accounts.csv")
        .map(stringToAccount)
        .filter(_.id == id)
        .headOption

      Right(maybeAccount)

    }
    catch {
      case err: Throwable =>
        Left(err)
    }

  }

  override def getMany(
      limit: Int
    ): Future[Either[Throwable, Set[Account]]] = ???

  override def createOne(
      item: Account
    ): Future[Either[Throwable, Account]] = ???

  override def createMany(
      items: Set[Account]
    ): Future[Either[Throwable, Set[Account]]] = ???

  override def updateById(
      id: String
    )(
      update: Account => Account
    ): Future[Either[Throwable, Account]] = ???

}
