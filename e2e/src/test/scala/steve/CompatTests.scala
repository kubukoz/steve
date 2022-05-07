package steve

import weaver.*
import cats.effect.IO
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.http4s.client.Client
import cats.effect.kernel.Async
import cats.implicits.*
import steve.client.ClientSideExecutor
import steve.server.Routing
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import steve.TestExecutor.TestResult

object CompatTests extends SimpleIOSuite {

  given Logger[IO] = NoOpLogger[IO]
  given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

  val goodBuild: Build = Build.empty
  val goodBuildResult: Hash = Hash(Vector.empty)

  val unknownHash = Hash(Vector(1))

  val unknownBaseBuild: Build = Build(
    Build.Base.ImageReference(unknownHash),
    Nil,
  )

  val unknownBaseError: Build.Error = Build.Error.UnknownBase(unknownHash)

  val unexpectedFailingBuild: Build = Build(
    Build.Base.EmptyImage,
    List(steve.Build.Command.Delete("k")),
  )

  val goodHash: Hash = Hash(Vector.empty)
  val unexpectedFailingHash: Hash = Hash(Vector(42))
  val goodRunResult: SystemState = SystemState(Map.empty)

  // todo :add logs
  val exec: Executor[IO] = TestExecutor.instance(
    Map(
      goodBuild -> TestResult.Success(goodBuildResult),
      unknownBaseBuild -> TestResult.Failure(unknownBaseError),
      unexpectedFailingBuild -> TestResult.Crash(new Throwable("build internal error")),
    ),
    Map(
      goodHash -> TestResult.Success(goodRunResult),
      // todo: add case for Failure (RunError)
      unexpectedFailingHash -> TestResult.Crash(new Throwable("run internal error")),
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

    client
      .build(goodBuild)
      .compile
      .toList
      .map(
        assert.eql(_, List(OutputEvent.Result(goodBuildResult.asRight)))
      )
  }

  test("Build image - unknown base error") {
    client
      .build(unknownBaseBuild)
      .compile
      .toList
      .map(result => assert.eql(result, List(OutputEvent.Result(unknownBaseError.asLeft))))
  }

  test("Build image - unexpected error") {

    client
      .build(unexpectedFailingBuild)
      .compile
      .toList
      .map { result =>
        assert.eql(result, List(OutputEvent.Failure(GenericServerError("Response stream error"))))
      }
  }

  test("Run hash - success") {
    client.run(goodHash).map(assert.eql(_, goodRunResult))
  }

  test("Run hash - unexpected error") {

    client
      .run(unexpectedFailingHash)
      .attempt
      .map(result => assert(result == GenericServerError("server failed").asLeft))
  }

  test("List images - success") {
    client.listImages.map(assert.eql(_, List(goodHash)))
  }

  // todo List images - internal error

}
