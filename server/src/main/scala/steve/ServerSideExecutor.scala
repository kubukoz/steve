package steve

import cats.implicits.*
import cats.MonadThrow
import cats.ApplicativeThrow

object ServerSideExecutor {

  def instance[F[_]: Interpreter: Resolver: MonadThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Vector.empty)
      private val emptySystem: SystemState = SystemState(Map.empty)

      def build(
        build: Build
      ): F[Hash] = Resolver[F]
        .resolve(build)
        .flatMap(Interpreter[F].interpret)
        .flatMap {
          case `emptySystem` => emptyHash.pure[F]
          case _             => new Throwable("Unsupported system").raiseError[F, Hash]
        }

      def run(hash: Hash): F[SystemState] = (hash == emptyHash)
        .guard[Option]
        .as(emptySystem)
        .liftTo[F](new Throwable("Unsupported hash!"))

    }

  def module[F[_]: MonadThrow]: Executor[F] = {
    given Interpreter[F] = Interpreter.instance[F]
    given Resolver[F] = Resolver.instance[F]
    instance[F]
  }

}
