package steve

import cats.effect.IO
import cats.implicits.*

object TestExecutor {

  enum TestResult[E, A] {
    case Success(a: A)
    case Failure(e: E)
    case Crash(e: Throwable)
  }

  def instance(
    buildImpl: Map[Build, TestResult[Build.Error, Hash]],
    runImpl: Map[Hash, TestResult[RunError, SystemState]],
  ): Executor[IO] =
    new Executor[IO] {

      def build(
        build: Build
      ): fs2.Stream[IO, OutputEvent[Either[Build.Error, Hash]]] =
        buildImpl(build) match {
          case TestResult.Success(r) =>
            fs2
              .Stream
              .emit(OutputEvent.Result(r.asRight))
          case TestResult.Failure(e) =>
            fs2
              .Stream
              .emit(OutputEvent.Result(e.asLeft))
          case TestResult.Crash(c) => fs2.Stream.raiseError(c)
        }

      def run(hash: Hash): IO[SystemState] =
        runImpl(hash) match {
          case TestResult.Success(a) => IO.pure(a)
          case TestResult.Failure(e) => IO.raiseError(e)
          case TestResult.Crash(c)   => IO.raiseError(c)
        }

      val listImages: IO[List[Hash]] = runImpl
        .collect { case (hash, TestResult.Success(state)) => hash }
        .toList
        .pure[IO]

    }

}
