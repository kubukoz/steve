package steve.server

import cats.implicits.*
import cats.Applicative
import cats.data.State
import monocle.syntax.all.*
import steve.SystemState
import steve.OutputEvent
import steve.Build

trait Interpreter[F[_]] {
  def interpret(build: ResolvedBuild): fs2.Stream[F, OutputEvent[Either[Build.Error, SystemState]]]
}

object Interpreter {
  def apply[F[_]](using F: Interpreter[F]): Interpreter[F] = F

  def instance[F[_]: Applicative]: Interpreter[F] =
    new Interpreter[F] {

      private val transition: ResolvedBuild.Command => State[SystemState, Unit] = {
        case ResolvedBuild.Command.Upsert(k, v) => State.modify(_.upsert(k, v))
        case ResolvedBuild.Command.Delete(k)    => State.modify(_.delete(k))
      }

      def interpret(
        build: ResolvedBuild
      ): fs2.Stream[F, OutputEvent[Either[Build.Error, SystemState]]] = fs2.Stream.emit {
        OutputEvent.Result {
          build
            .commands
            .traverse(transition)
            .runS(build.base)
            .value
            .asRight
        }
      }

    }

}
