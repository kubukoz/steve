package steve

import weaver.*
import weaver.scalacheck.Checkers
import Arbitraries.given
import cats.effect.IO
import steve.Build.Base
import steve.Build.Error.UnknownBase

object ResolverTests extends SimpleIOSuite with Checkers {

  given Hasher[F] = Hasher.sha256Hasher[F]

  test("resolve(any build basing on the empty image)") {
    forall {
      (
        emptyHash: Hash,
        emptySystem: SystemState,
        commands: List[Build.Command],
      ) =>
        Registry.inMemory[IO](Map(Registry.emptyHash -> emptySystem)).use { registry =>

          given Registry[IO] = registry

          Resolver
            .instance[IO]
            .resolve(Build(Base.EmptyImage, commands))
            .map { resolved =>
              val newBase = resolved.base

              assert(newBase == emptySystem)
            }
        }
    }
  }

  test("registry.save(system) >>= resolve == system") {
    forall { (system: SystemState, commands: List[Build.Command]) =>
      Registry.inMemory[IO](Map.empty).use { registry =>
        given Registry[IO] = registry
        val resolver = Resolver.instance[IO]

        registry.save(system).flatMap { baseHash =>
          val build = Build(Base.ImageReference(baseHash), commands)

          resolver
            .resolve(build)
            .map { resolved =>
              val newBase = resolved.base

              assert(newBase == system)
            }
        }
      }
    }
  }

  test("resolve(unknown hash) fails") {
    forall { (system: SystemState, commands: List[Build.Command], hash: Hash) =>
      Registry.inMemory[IO](Map.empty).use { registry =>
        given Registry[IO] = registry
        val resolver = Resolver.instance[IO]

        val build = Build(Base.ImageReference(hash), commands)

        resolver
          .resolve(build)
          .attempt
          .map { resolved =>
            assert(resolved == Left(UnknownBase(hash)))
          }
      }
    }
  }
}
