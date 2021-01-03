package eu.bluelabs
package test

import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait TestSuite
    extends AnyFunSuite
      with Matchers
      with BeforeAndAfter
      with BeforeAndAfterEach
      with ScalaCheckPropertyChecks {

  override implicit val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 10)

}
