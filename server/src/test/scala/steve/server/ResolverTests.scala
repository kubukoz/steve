package steve.server

import weaver.*
import weaver.scalacheck.Checkers
import Arbitraries.given
import cats.effect.IO
import steve.Build.Base
import steve.Build.Error.UnknownBase
import cats.effect.kernel.Resource
import cats.Applicative
import cats.effect.implicits.*
import steve.Build
import steve.SystemState
import steve.Hash

object ResolverTests extends SimpleIOSuite with Checkers {

  given Hasher[IO] = Hasher.sha256Hasher[IO]

  val unit = Applicative[IO].unit

  test("resolve(any build basing on the empty image)") {
    forall {
      (
        commands: List[Build.Command],
      ) =>
        for {
          given Registry[IO] <- Registry.inMemory[IO]
          build = Build(Base.EmptyImage, commands)
          resolved <- Resolver.instance[IO].resolve(build)
          newBase = resolved.base
        } yield assert(newBase == SystemState.empty)
    }
  }

  test("registry.save(system) >>= resolve == system") {
    forall { (system: SystemState, commands: List[Build.Command]) =>
      for {
        given Registry[IO] <- Registry.inMemory[IO]
        _ <- unit
        resolver = Resolver.instance[IO]

        baseHash <- Registry[IO].save(system)
        build = Build(Base.ImageReference(baseHash), commands)
        resolved <- resolver.resolve(build)

        newBase = resolved.base
      } yield assert(newBase == system)
    }
  }

  test("resolve(unknown hash) fails") {
    forall { (system: SystemState, commands: List[Build.Command], hash: Hash) =>
      for {
        given Registry[IO] <- Registry.inMemory[IO]
        _ <- unit
        resolver = Resolver.instance[IO]

        build = Build(Base.ImageReference(hash), commands)
        resolved <- resolver.resolve(build).attempt
      } yield assert(resolved == Left(UnknownBase(hash)))
    }
  }
}
