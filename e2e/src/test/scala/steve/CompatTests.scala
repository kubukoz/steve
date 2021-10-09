package steve

import munit.CatsEffectSuite
import cats.effect.IO
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.http4s.client.Client
import cats.effect.kernel.Async
import cats.implicits._

class CompatTests extends CatsEffectSuite {

  def testExecutor(
    buildImpl: Map[Build, Either[Throwable, Hash]],
    runImpl: Map[Hash, Either[Throwable, SystemState]],
  ): Executor[IO] =
    new Executor[IO] {
      def build(build: Build): IO[Hash] = buildImpl(build).liftTo[IO]
      def run(hash: Hash): IO[SystemState] = runImpl(hash).liftTo[IO]
    }

  given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

  val goodBuild: Build = Build.empty
  val goodBuildResult: Hash = Hash(Vector.empty)

  val unexpectedFailingBuild: Build = Build(
    Build.Base.EmptyImage,
    List(steve.Build.Command.Delete("k")),
  )

  val goodHash: Hash = Hash(Vector.empty)
  val unexpectedFailingHash: Hash = Hash(Vector(42))
  val goodRunResult: SystemState = SystemState(Map.empty)

  val exec: Executor[IO] = testExecutor(
    Map(
      goodBuild -> goodBuildResult.asRight,
      unexpectedFailingBuild -> new Throwable("build internal error").asLeft,
    ),
    Map(
      goodHash -> goodRunResult.asRight,
      unexpectedFailingHash -> new Throwable("run internal error").asLeft,
    ),
  )

  val client = ClientSideExecutor.instance[IO](
    Client.fromHttpApp(
      Routing.instance[IO](
        exec
      )
    )
  )

  test("Build image - success") {

    assertIO(
      client.build(goodBuild),
      goodBuildResult,
    )
  }

  test("Build image - unexpected error") {

    assertIO(
      client.build(unexpectedFailingBuild).attempt,
      GenericServerError("server failed").asLeft,
    )
  }

  test("Run hash - success") {

    assertIO(
      client.run(goodHash),
      goodRunResult,
    )
  }

  test("Run hash - unexpected error") {

    assertIO(
      client.run(unexpectedFailingHash).attempt,
      GenericServerError("server failed").asLeft,
    )
  }
}
