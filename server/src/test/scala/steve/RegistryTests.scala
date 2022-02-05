package steve

import cats.Id
import cats.catsInstancesForId
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import ResolvedBuild.Command.*
import Arbitraries.given
import cats.effect.IO
import munit.CatsEffectSuite

class RegistryTests extends CatsEffectSuite with ScalaCheckSuite {

  // val registry = Registry.inMemory[IO]

  // property("save + lookup returns the same system") {
  //   forAll {
  //     (
  //       system: SystemState,
  //     ) =>
  //       assertIO(IO(true), false)
  //   }
  // }

}
