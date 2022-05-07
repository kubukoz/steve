package steve.client

import cats.effect.MonadCancelThrow
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import cats.implicits.*
import sttp.tapir.PublicEndpoint
import org.http4s.Status
import org.http4s.implicits.*
import steve.Executor
import steve.protocol
import steve.Build
import steve.GenericServerError
import steve.SystemState
import steve.Hash
import sttp.capabilities.fs2.Fs2Streams
import steve.OutputEvent
import cats.effect.kernel.Sync
import cats.effect.kernel.Resource
import org.typelevel.log4cats.Logger

object ClientSideExecutor {

  def instance[F[_]: Http4sClientInterpreter: Sync: Logger](
    client: Client[F]
  )(
    using fs2.Compiler[F, F]
  ): Executor[F] =
    new Executor[F] {

      private def run[I, E <: Throwable, O](
        endpoint: PublicEndpoint[I, E, O, Fs2Streams[F]],
        input: I,
      ): Resource[F, O] = {
        val (req, handler) = summon[Http4sClientInterpreter[F]]
          .toRequestThrowDecodeFailures(endpoint, Some(uri"http://localhost:8080"))
          .apply(input)

        client
          .run(req)
          .evalMap {
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

      def build(build: Build): fs2.Stream[F, OutputEvent[Either[Build.Error, Hash]]] =
        fs2.Stream.resource(run(protocol.build, build)).flatten

      def run(hash: Hash): F[SystemState] = run(protocol.run, hash).use(_.pure[F])
      val listImages: F[List[Hash]] = run(protocol.listImages, ()).use(_.pure[F])
    }

}
