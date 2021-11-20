package steve

import cats.Applicative
import cats.implicits.*
import cats.data.State
import monocle.syntax.all._

trait Interpreter[F[_]] {
  def build(build: ResolvedBuild): F[SystemState]
}

object Interpreter {

  def apply[F[_]](using F: Interpreter[F]): Interpreter[F] = F

  def instance[F[_]: Applicative]: Interpreter[F] =
    new Interpreter[F] {

      private def transition(cmd: ResolvedBuild.Command): State[SystemState, Unit] =
        cmd match {
          case ResolvedBuild.Command.Upsert(k, v) =>
            State.modify { sys =>
              sys.focus(_.all).modify(_ + (k -> v))
            }
          case ResolvedBuild.Command.Delete(k) =>
            State.modify { sys =>
              sys.focus(_.all).modify(_ - k)
            }
        }

      def build(build: ResolvedBuild): F[SystemState] = build
        .commands
        .traverse(transition)
        .runS(build.base)
        .value
        .pure[F]

    }

}
