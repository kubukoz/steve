package steve

import cats.implicits.*
import cats.MonadThrow

object ServerSideExecutor {

  def instance[F[_]: Interpreter: MonadThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Vector.empty)
      private val emptySystem: SystemState = SystemState(Map.empty)

      private val resolveCommand: Build.Command => ResolvedBuild.Command = {
        case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
        case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)
      }

      private def resolve(build: Build): F[ResolvedBuild] = (build == Build.empty)
        .guard[Option]
        .as(emptySystem)
        .liftTo[F](new Throwable("Unsupported build!"))
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

      def build(build: Build): F[Hash] = resolve(build).flatMap(Interpreter[F].interpret).flatMap {
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
    instance[F]
  }

}
