package steve

import cats.effect.MonadCancelThrow
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.implicits.*
import sttp.tapir.PublicEndpoint
import org.http4s.Status
import org.http4s.implicits.*

object ClientSideExecutor {

  def instance[F[_]: Http4sClientInterpreter: MonadCancelThrow](
    client: Client[F]
  )(
    using fs2.Compiler[F, F]
  ): Executor[F] =
    new Executor[F] {

      private def run[I, E <: Throwable, O](
        endpoint: PublicEndpoint[I, E, O, Any],
        input: I,
      ): F[O] = {
        val (req, handler) = summon[Http4sClientInterpreter[F]]
          .toRequestUnsafe(endpoint, Some(uri"http://localhost:8080"))
          .apply(input)

        client
          .run(req)
          .use {
            case r if r.status == Status.InternalServerError =>
              r
                .bodyText
                .compile
                .string
                .flatMap(io.circe.parser.decode[GenericServerError](_).liftTo[F])
                .flatMap(_.raiseError[F, O])
            case r => handler(r).rethrow
          }
      }

      def build(build: Build): F[Hash] = run(protocol.build, build)

      def run(hash: Hash): F[SystemState] = run(protocol.run, hash)
    }

}
