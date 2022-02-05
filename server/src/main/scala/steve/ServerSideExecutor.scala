package steve

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

object ServerSideExecutor {

  def instance[F[_]: Interpreter: Resolver: Registry: MonadThrow]: Executor[F] =
    new Executor[F] {

      // test idea:
      // for x : emptyimage + any commands |> build
      //
      // build(x).flatMap(run).isSuccess
      // build(x) <-> build(x)
      def build(
        build: Build
      ): F[Hash] = Resolver[F]
        .resolve(build)
        .flatMap(Interpreter[F].interpret)
        .flatMap(Registry[F].save)

      def run(
        hash: Hash
      ): F[SystemState] = Registry[F]
        .lookup(hash)
        .flatMap(_.liftTo[F](UnknownHash(hash)))

    }

  def module[F[_]: Sync]: Resource[F, Executor[F]] = {
    val unit = Applicative[F].unit.toResource

    given Interpreter[F] = Interpreter.instance[F]
    given Hasher[F] = Hasher.sha256Hasher[F]

    for {
      given Registry[F] <- Registry.instance[F]
      _ <- unit
      given Resolver[F] = Resolver.instance[F]
    } yield instance[F]
  }

}
