package steve

import munit.CatsEffectSuite
import cats.effect.IO
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.http4s.client.Client
import cats.effect.kernel.Async
import cats.implicits.*
import steve.client.ClientSideExecutor
import steve.server.Routing

class CompatTests extends CatsEffectSuite {

  given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

  val goodBuild: Build = Build.empty
  val goodBuildResult: Hash = Hash(Vector.empty)

  val unknownHash = Hash(Vector(1))

  val unknownBaseBuild: Build = Build(
    Build.Base.ImageReference(unknownHash),
    Nil,
  )

  val unknownBaseError: Throwable = Build.Error.UnknownBase(unknownHash)

  val unexpectedFailingBuild: Build = Build(
    Build.Base.EmptyImage,
    List(steve.Build.Command.Delete("k")),
  )

  val goodHash: Hash = Hash(Vector.empty)
  val unexpectedFailingHash: Hash = Hash(Vector(42))
  val goodRunResult: SystemState = SystemState(Map.empty)

  val exec: Executor[IO] = TestExecutor.instance(
    Map(
      goodBuild -> goodBuildResult.asRight,
      unknownBaseBuild -> unknownBaseError.asLeft,
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

  test("Build image - unknown base error") {
    assertIO(
      client.build(unknownBaseBuild).attempt,
      unknownBaseError.asLeft,
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

  test("List images - success") {
    assertIO(
      client.listImages,
      List(goodHash),
    )
  }

  // todo List images - internal error

}
