package steve.server

import cats.implicits.*
import cats.Applicative
import steve.SystemState
import steve.OutputEvent
import steve.Build
import cats.effect.kernel.Ref

trait Interpreter[F[_]] {
  def interpret(build: ResolvedBuild): fs2.Stream[F, OutputEvent[Either[Build.Error, SystemState]]]
}

object Interpreter {
  def apply[F[_]](using F: Interpreter[F]): Interpreter[F] = F

  def instance[F[_]: Applicative: Ref.Make]: Interpreter[F] =
    new Interpreter[F] {

      private def transition(
        stateRef: Ref[F, SystemState]
      ): ResolvedBuild.Command => F[OutputEvent.LogMessage[Nothing]] = {
        case ResolvedBuild.Command.Upsert(k, v) =>
          stateRef
            .update(_.upsert(k, v))
            .as(OutputEvent.LogMessage(s"Upserting $k: $v"))

        case ResolvedBuild.Command.Delete(k) =>
          stateRef
            .update(_.delete(k))
            .as(OutputEvent.LogMessage(s"Deleting $k"))
      }

      def interpret(
        build: ResolvedBuild
      ): fs2.Stream[F, OutputEvent[Either[Build.Error, SystemState]]] = fs2
        .Stream
        .eval(Ref[F].of(build.base))
        .flatMap { stateRef =>
          val lhs = fs2
            .Stream
            .emits(build.commands)
            .evalMap(transition(stateRef))

          val rhs = fs2.Stream.eval(stateRef.get).map(OutputEvent.Result(_))

          lhs ++ rhs
        }
        .map(_.map(_.asRight))

    }

}
