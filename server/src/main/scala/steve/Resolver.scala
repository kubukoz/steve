package steve

import cats.MonadThrow
import cats.effect.implicits.*
import cats.implicits.*
import steve.Build.Error.UnknownBase

trait Resolver[F[_]] {
  def resolve(build: Build): F[ResolvedBuild]
}

object Resolver {
  def apply[F[_]](using ev: Resolver[F]): Resolver[F] = ev

  def instance[F[_]: MonadThrow: Registry]: Resolver[F] =
    new Resolver[F] {

      private val resolveCommand: Build.Command => ResolvedBuild.Command = {
        case Build.Command.Upsert(k, v) => ResolvedBuild.Command.Upsert(k, v)
        case Build.Command.Delete(k)    => ResolvedBuild.Command.Delete(k)
      }

      private def resolveBase(base: Build.Base): F[SystemState] =
        base match {
          case Build.Base.EmptyImage =>
            Registry[F]
              .lookup(Registry.emptyHash)
              .flatMap(_.liftTo[F](new Throwable("Impossible! Hash not found for empty system")))

          case Build.Base.ImageReference(hash) =>
            Registry[F]
              .lookup(hash)
              .flatMap(_.liftTo[F](UnknownBase(hash)))
        }

      def resolve(build: Build): F[ResolvedBuild] = resolveBase(build.base)
        .map { sys =>
          ResolvedBuild(sys, build.commands.map(resolveCommand))
        }

    }

}
