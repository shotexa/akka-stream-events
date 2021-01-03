package eu.bluelabs
package repositories

import scala.concurrent.Future

import entities._

trait Repository[T] {
  def getById(id: String): Future[Either[Throwable, Option[T]]]
  def getMany(limit: Int = 10): Future[Either[Throwable, Set[T]]]
  def createOne(item: T): Future[Either[Throwable, T]]
  def createMany(items: Set[T]): Future[Either[Throwable, Set[T]]]
  def updateById(id: String)(update: T => T): Future[Either[Throwable, T]]

}
