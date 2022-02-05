package steve

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

object Arbitraries {

  given Arbitrary[SystemState] = Arbitrary(Gen.resultOf(SystemState.apply))

  given Arbitrary[ResolvedBuild.Command] = Arbitrary {
    Gen.oneOf(
      Gen.resultOf(ResolvedBuild.Command.Upsert.apply),
      Gen.resultOf(ResolvedBuild.Command.Delete.apply),
    )
  }

}
