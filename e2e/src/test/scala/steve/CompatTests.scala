package steve

import munit.CatsEffectSuite
import steve.ClientSideExecutor
import cats.effect.IO
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.http4s.client.Client
import steve.Build
import steve.Routing
import steve.ServerSideExecutor
import cats.implicits.*
import steve.Hash
import cats.ApplicativeThrow

class CompatTests extends CatsEffectSuite {

  given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

  def testExecutor[F[_]: ApplicativeThrow](
    builds: Map[Build, Either[Throwable, Hash]],
    runs: Map[Hash, Either[Throwable, SystemState]],
  ): Executor[F] =
    new Executor[F] {

      def build(build: Build): F[Hash] = builds(build).liftTo[F]

      def run(hash: Hash): F[SystemState] = runs(hash).liftTo[F]
    }

  val goodBuild = Build.empty
  val goodHash = Hash(Vector.empty)
  val badHash = Hash(Vector(Byte.MaxValue))

  val goodHashResult = SystemState(Map("k1" -> "v1"))
  val badHashResult = GenericServerError(message = "Unknown hash")

  val client = Client.fromHttpApp[IO](
    Routing.instance(
      testExecutor(
        builds = Map(
          goodBuild -> goodHash.asRight
        ),
        runs = Map(
          goodHash -> goodHashResult.asRight,
          badHash -> badHashResult.asLeft,
        ),
      )
    )
  )

  val exec = ClientSideExecutor.instance[IO](client)

  test("build a build - success case") {
    assertIO(
      exec.build(Build.empty),
      goodHash,
    )
  }

  test("run a hash - success case") {
    assertIO(
      exec.run(goodHash),
      goodHashResult,
    )
  }

  test("run a hash - generic error case") {
    assertIO(
      exec.run(badHash).attempt,
      Left(badHashResult),
    )
  }
}
