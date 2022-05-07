package steve

import cats.effect.IO
import cats.implicits.*

object TestExecutor {

  def instance(
    buildImpl: Map[Build, Either[Throwable, Hash]],
    runImpl: Map[Hash, Either[Throwable, SystemState]],
  ): Executor[IO] =
    new Executor[IO] {

      def build(
        build: Build
      ): fs2.Stream[IO, OutputEvent[Hash]] = fs2
        .Stream
        .eval(buildImpl(build).liftTo[IO])
        .map(OutputEvent.Result(_))

      def run(hash: Hash): IO[SystemState] = runImpl(hash).liftTo[IO]

      val listImages: IO[List[Hash]] = runImpl
        .collect { case (hash, Right(state)) => hash }
        .toList
        .pure[IO]

    }

}
