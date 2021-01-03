package eu.bluelabs
package test

class ExampleTestSuite extends TestSuite {
  test("example based test") {
    1 shouldBe 1
  }

  test("property based test") {
    forAll { n: Int =>
      n % 1 shouldBe 0 
    }
  }

}
