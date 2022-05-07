package steve

import sttp.tapir.Endpoint
import io.circe.Codec as CirceCodec
import sttp.model.StatusCode
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.kernel.Temporal
import io.circe.syntax.*
import scala.concurrent.duration.*
import cats.effect.kernel.Sync
import org.typelevel.log4cats.*

object protocol {
  import sttp.tapir.*
  import sttp.tapir.json.circe.*

  private val base = infallibleEndpoint.in("api")

  def build[F[_]: Sync: Logger]: PublicEndpoint[Build, Nothing, fs2.Stream[
    F,
    OutputEvent[Either[Build.Error, Hash]],
  ], Fs2Streams[F]] = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(outputEventStream[F, Either[Build.Error, Hash]])

  val run: PublicEndpoint[Hash, RunError, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])
    .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[RunError]))

  val listImages: PublicEndpoint[Unit, Nothing, List[Hash], Any] = base
    .get
    .in("images")
    .out(jsonBody[List[Hash]])

  private def outputEventStream[F[_]: Sync: Logger, A: CirceCodec: Schema] =
    streamBody(Fs2Streams[F])(
      summon[Schema[List[OutputEvent[A]]]],
      CodecFormat.Json(),
    )
      .map {
        _.through(io.circe.fs2.byteArrayParser)
          .through(io.circe.fs2.decoder[F, OutputEvent[A]])
      } { events =>
        fs2
          .Stream
          .emit("[")
          .append(
            events
              .handleErrorWith { e =>
                fs2.Stream.exec(Logger[F].error(e)("Response stream error")) ++
                  fs2
                    .Stream
                    .emit(OutputEvent.Failure(GenericServerError("Response stream error")))
              }
              .map(_.asJson.noSpaces)
              .intersperse(",")
          )
          .append(fs2.Stream.emit("]"))
          .through(fs2.text.utf8.encode)
      }

}
