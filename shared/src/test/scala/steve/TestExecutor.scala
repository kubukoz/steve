package steve

import cats.effect.IO
import cats.implicits.*

object TestExecutor {

  def instance(
    buildImpl: Map[Build, Either[Throwable, Hash]],
    runImpl: Map[Hash, Either[Throwable, SystemState]],
  ): Executor[IO] =
    new Executor[IO] {
      def build(build: Build): IO[Hash] = buildImpl(build).liftTo[IO]
      def run(hash: Hash): IO[SystemState] = runImpl(hash).liftTo[IO]
    }

}
