package steve

import weaver.*
import weaver.discipline.*
import cats.kernel.laws.discipline.EqTests
import cats.laws.discipline.TraverseTests

import cats.data.Validated
import cats.data.ZipLazyList
import org.scalacheck.Arbitrary
import cats.laws.discipline.arbitrary.*
import org.scalacheck.Gen

object OutputEventTests extends FunSuite with Discipline {

  given Arbitrary[GenericServerError] = Arbitrary(Gen.resultOf(GenericServerError.apply))

  given [A: Arbitrary]: Arbitrary[OutputEvent[A]] = Arbitrary {
    Gen.oneOf(
      Gen.resultOf(OutputEvent.Result[A].apply),
      Gen.resultOf(OutputEvent.LogMessage.apply),
      Gen.resultOf(OutputEvent.Failure.apply),
    )
  }

  checkAll(
    "Traverse[OutputEvent]",
    TraverseTests[OutputEvent]
      .traverse[Int, String, Boolean, Int, Option, Validated[Int, *]],
  )
}
