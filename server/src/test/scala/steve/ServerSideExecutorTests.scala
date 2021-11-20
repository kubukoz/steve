package steve

import munit.CatsEffectSuite
import cats.Id

class ServerSideExecutorTests extends CatsEffectSuite {
  val exec = ServerSideExecutor.module[Either[Throwable, *]]

  test("Build and run empty image") {

    assertEquals(
      exec.build(Build.empty).flatMap(exec.run).map(_.all),
      Right(Map.empty),
    )
  }
}
