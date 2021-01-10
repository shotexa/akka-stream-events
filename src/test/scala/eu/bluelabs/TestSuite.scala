package eu.bluelabs
package test

import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.funspec.AnyFunSpec

trait TestSuite
    extends AnyFunSpec
      with PrivateMethodTester
      with Matchers
      with BeforeAndAfter
      with BeforeAndAfterEach
      with BeforeAndAfterAll
      with ScalaCheckPropertyChecks {

  override implicit val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 10)

}
