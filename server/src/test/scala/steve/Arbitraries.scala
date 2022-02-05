package steve

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

object Arbitraries {

  given Arbitrary[SystemState] = Arbitrary(Gen.resultOf(SystemState.apply))

  given Arbitrary[Build.Command] = Arbitrary {
    Gen.oneOf(
      Gen.resultOf(Build.Command.Upsert.apply),
      Gen.resultOf(Build.Command.Delete.apply),
    )
  }

  given arbResolvedCommand: Arbitrary[ResolvedBuild.Command] = Arbitrary {
    Gen.oneOf(
      Gen.resultOf(ResolvedBuild.Command.Upsert.apply),
      Gen.resultOf(ResolvedBuild.Command.Delete.apply),
    )
  }

  given Arbitrary[Hash] = Arbitrary {
    Gen.resultOf(Hash.apply)
  }

}
