package steve.server

import weaver.*
import cats.Id
import cats.effect.SyncIO
import cats.effect.IO
import cats.implicits.*
import steve.Build
import steve.OutputEvent
import steve.Hash

object ServerSideExecutorTests extends SimpleIOSuite {

  val execR = ServerSideExecutor.module[IO]

  test("Build empty image") {
    fs2
      .Stream
      .resource(execR)
      .flatMap(_.build(Build.empty))
      .compile
      .toList
      .map { logs =>
        assert.eql(
          logs.map(_.map(_.map(_.toHex))),
          List(
            OutputEvent.Result(
              "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855".asRight
            )
          ),
        )
      }
  }

  test("Build and run empty image") {
    execR
      .use(exec => OutputEvent.getResult(exec.build(Build.empty)).rethrow.flatMap(exec.run))
      .map(_.all)
      .map(
        assert.eql(_, Map.empty)
      )
  }
}
