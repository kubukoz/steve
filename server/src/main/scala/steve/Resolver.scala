package steve

import cats.implicits.*
import cats.MonadThrow
import cats.ApplicativeThrow

trait Resolver[F[_]] {
  def resolve(build: Build): F[ResolvedBuild]
}

object Resolver {
  def apply[F[_]](using ev: Resolver[F]): Resolver[F] = ev

  def instance[F[_]: ApplicativeThrow]: Resolver[F] =
    new Resolver[F] {
      private val emptyHash: Hash = Hash(Vector.empty)
      private val emptySystem: SystemState = SystemState(Map.empty)

      private val resolveCommand: Build.Command => ResolvedBuild.Command = {
        case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
        case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)
      }

      def resolve(build: Build): F[ResolvedBuild] = (build == Build.empty)
        .guard[Option]
        .as(emptySystem)
        .liftTo[F](new Throwable("Unsupported build!"))
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

    }

}
