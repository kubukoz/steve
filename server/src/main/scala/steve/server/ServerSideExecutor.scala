package steve.server

import cats.Applicative
import cats.ApplicativeThrow
import cats.MonadThrow
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.UUIDGen
import cats.implicits.*
import steve.Build.Error.UnknownHash
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
      def build(build: Build): fs2.Stream[F, OutputEvent[Either[Build.Error, Hash]]] =
        // todo: output actual events
        // fs2
        //   .Stream(
        //     "hello world",
        //     "goodbye world",
        //   )
        //   .map(OutputEvent.LogMessage(_))
        //   ++
        fs2.Stream.eval {
          Resolver[F]
            .resolve(build)
            .flatMap(Interpreter[F].interpret)
            .flatMap(Registry[F].save)
            .attemptNarrow[Build.Error]
            .map(OutputEvent.Result(_))
        }

      def run(
        hash: Hash
      ): F[SystemState] = Registry[F]
        .lookup(hash)
        .flatMap(_.liftTo[F](RunError.UnknownHash(hash)))

      val listImages: F[List[Hash]] = Registry[F].list
    }

  def module[F[_]: Sync]: Resource[F, Executor[F]] = {
    val unit = Applicative[F].unit.toResource

    given Interpreter[F] = Interpreter.instance[F]
    given Hasher[F] = Hasher.sha256Hasher[F]

    for {
      given Registry[F] <- Registry.instance[F].toResource
      _ <- unit
      given Resolver[F] = Resolver.instance[F]
    } yield instance[F]
  }

}
