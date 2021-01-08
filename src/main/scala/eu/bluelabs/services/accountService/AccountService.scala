package eu.bluelabs
package services
package accountService

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration._

import repositories.Repository
import entities._

class AccountService private (db: Repository[Account])
    extends AccountServiceApi {

  override def getAccount(
      accountId: String
    ): Future[Either[AccountServiceError, Account]] =
    db.getById(accountId).map {
      case Left(err) => Left(AccountServiceError(err.getMessage, Some(err)))
      case Right(acc) =>
        acc match {
          case None =>
            Left(
              AccountServiceError(
                s"Account with an id of $accountId not found",
                None
              )
            )
          case Some(acc) => Right(acc)
        }
    }

}

object AccountService {
  def apply(db: Repository[Account]): AccountService = new AccountService(db)
}
