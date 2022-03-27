package steve.server

import munit.CatsEffectSuite
import cats.Id
import cats.effect.SyncIO
import cats.effect.IO
import steve.Build

class ServerSideExecutorTests extends CatsEffectSuite {

  val execR = ServerSideExecutor.module[IO]

  test("Build and run empty image") {

    assertIO(
      execR.use(exec => exec.build(Build.empty).flatMap(exec.run)).map(_.all),
      Map.empty,
    )
  }

}
