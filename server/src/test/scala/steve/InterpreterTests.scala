package steve

import munit.ScalaCheckSuite
import cats.Id
import cats.catsInstancesForId
import org.scalacheck.Prop._
import org.scalacheck.Arbitrary

class InterpreterTests extends ScalaCheckSuite {
  val interpreter: Interpreter[Id] = Interpreter.instance

  given Arbitrary[SystemState] = Arbitrary {
    Arbitrary.arbitrary[Map[String, String]].map(SystemState(_))
  }

  property("upsert any key to any system") {
    forAll { (k: String, v: String, sys: SystemState) =>
      val result = interpreter.build(
        ResolvedBuild(sys, List(ResolvedBuild.Command.Upsert(k, v)))
      )

      assertEquals(result.all.get(k), Some(v))
    }
  }

  property("delete any key from any system") {
    forAll { (k: String, sys: SystemState) =>
      val result = interpreter.build(
        ResolvedBuild(sys, List(ResolvedBuild.Command.Delete(k)))
      )

      assertEquals(result.all.get(k), None)
    }
  }

  // todo: more tests

  // split build == build together
}
