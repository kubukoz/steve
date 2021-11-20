package steve

import cats.implicits.*
import cats.MonadThrow

object ServerSideExecutor {

  def instance[F[_]: MonadThrow: Interpreter]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Vector.empty)
      private val emptySystem: SystemState = SystemState(Map.empty)

      private def resolveCommand(cmd: Build.Command): ResolvedBuild.Command =
        cmd match {
          case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
          case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)
        }

      private def resolve(build: Build): F[ResolvedBuild] = (build.base == Build.Base.EmptyImage)
        .guard[Option]
        .liftTo[F](new Throwable("Unsupported base!"))
        .as(emptySystem)
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

      def build(build: Build): F[Hash] = resolve(build).flatMap(Interpreter[F].build).flatMap {
        case `emptySystem` => emptyHash.pure[F]
        case _             => new Throwable("Unsupported system state!").raiseError[F, Hash]
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
