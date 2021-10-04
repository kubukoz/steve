package steve

import cats.effect.MonadCancelThrow
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.implicits.*
import sttp.tapir.Endpoint
import sttp.tapir.DecodeResult
import org.http4s.Status

object ClientSideExecutor {

  def instance[F[_]: Http4sClientInterpreter: MonadCancelThrow](
    client: Client[F]
  )(
    using fs2.Compiler[F, F]
  ): Executor[F] =
    new Executor[F] {

      private def run[I, E <: Throwable, O](endpoint: Endpoint[I, E, O, Any], input: I): F[O] = {
        val (req, handler) = summon[Http4sClientInterpreter[F]]
          .toRequestUnsafe(endpoint, Some("http://localhost:8080"))
          .apply(input)

        client.run(req).use {
          case response if response.status == Status.InternalServerError =>
            response
              .bodyText
              .compile
              .string
              .flatMap(io.circe.parser.decode[GenericServerError](_).liftTo[F])
              .flatMap(_.raiseError)

          case response =>
            handler(response).rethrow
        }
      }

      def build(build: Build): F[Hash] = run(protocol.build, build)

      def run(hash: Hash): F[SystemState] = run(protocol.run, hash)
    }

}
