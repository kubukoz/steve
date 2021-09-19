package steve

import cats.effect.MonadCancelThrow
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.implicits.*
import sttp.tapir.Endpoint

object ClientSideExecutor {

  def instance[F[_]: Http4sClientInterpreter: MonadCancelThrow](client: Client[F]): Executor[F] =
    new Executor[F] {

      private def run[I, E <: Throwable, O](endpoint: Endpoint[I, E, O, Any], input: I): F[O] = {
        val (req, handler) = summon[Http4sClientInterpreter[F]]
          .toRequestUnsafe(endpoint, Some("http://localhost:8080"))
          .apply(input)

        client.run(req).use(handler).rethrow
      }

      def build(build: Build): F[Hash] = run(protocol.build, build)

      def run(hash: Hash): F[SystemState] = run(protocol.run, hash)
    }

}
