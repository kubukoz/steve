package steve

import cats.Id
import cats.catsInstancesForId
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import ResolvedBuild.Command.*
import Arbitraries.given
import cats.effect.IO
import weaver.*
import weaver.scalacheck.Checkers
import cats.implicits.*

object RegistryTests extends SimpleIOSuite with Checkers {

  given Hasher[F] = Hasher.sha256Hasher[F]
  val registryR = Registry.instance[IO]

  test("save + lookup returns the same system") {
    forall { (system: SystemState) =>
      registryR.use { registry =>
        for {
          hash <- registry.save(system)
          result <- registry.lookup(hash)
        } yield assert(result.contains(system))
      }
    }
  }

  test("save is not affected by other writes") {
    forall { (system: SystemState, otherSystems: List[SystemState]) =>
      registryR.use { registry =>
        for {
          hash <- registry.save(system)
          _ <- otherSystems.traverse_(registry.save)
          hash2 <- registry.save(system)
        } yield assert(hash == hash2)
      }
    }
  }

  test("lookup is idempotent") {
    forall {
      (
        systems: List[SystemState],
        moreSystems: List[SystemState],
        hash: Hash,
      ) =>
        registryR.use { registry =>
          for {
            _ <- systems.traverse_(registry.save)
            result1 <- registry.lookup(hash)

            _ <- moreSystems.traverse_(registry.save)
            result2 <- registry.lookup(hash)
          } yield assert(result1 == result2)
        }
    }
  }

}
