package steve.server

import cats.MonadThrow
import cats.effect.implicits.*
import cats.effect.kernel.Resource
import cats.implicits.*
import cats.effect.kernel.Sync
import steve.Build
import steve.Executor
import steve.Hash
import steve.SystemState
import steve.OutputEvent
import steve.RunError

object ServerSideExecutor {

  def instance[F[_]: Interpreter: Resolver: Registry: MonadThrow]: Executor[F] =
    new Executor[F] {

      // test idea:
      // for x : emptyimage + any commands |> build
      //
      // build(x).flatMap(run).isSuccess
      // build(x) <-> build(x)
      def build(build: Build): fs2.Stream[F, OutputEvent[Either[Build.Error, Hash]]] = fs2
        .Stream
        .eval {
          Resolver[F]
            .resolve(build)
            .attemptNarrow[Build.Error]
        }
        .flatMap { resolvedBuild =>
          resolvedBuild match {
            case Right(v) => Interpreter[F].interpret(v)
            case Left(e)  => fs2.Stream.emit(OutputEvent.Result(Left(e)))
          }
        }
        .evalMap(_.traverse(_.traverse(Registry[F].save)))

      def run(
        hash: Hash
      ): F[SystemState] = Registry[F]
        .lookup(hash)
        .flatMap(_.liftTo[F](RunError.UnknownHash(hash)))

      val listImages: F[List[Hash]] = Registry[F].list
    }

  def module[F[_]: Sync]: Resource[F, Executor[F]] = {

    given Interpreter[F] = Interpreter.instance[F]
    given Hasher[F] = Hasher.sha256Hasher[F]

    for {
      given Registry[F] <- Registry.instance[F].toResource
      given Resolver[F] = Resolver.instance[F]
    } yield instance[F]
  }

}
