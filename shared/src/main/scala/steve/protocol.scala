package steve

import sttp.tapir.Endpoint
import io.circe.Codec as CirceCodec
import sttp.model.StatusCode
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.kernel.Temporal
import io.circe.syntax.*
import scala.concurrent.duration.*
import cats.effect.kernel.Sync

object protocol {
  import sttp.tapir.*
  import sttp.tapir.json.circe.*

  private val base = infallibleEndpoint.in("api")

  def build[
    F[_]: Sync
  ]: PublicEndpoint[Build, steve.Build.Error, fs2.Stream[F, OutputEvent[Hash]], Fs2Streams[F]] =
    base
      .put
      .in("build")
      .in(jsonBody[Build])
      .out(
        // todo: extract
        streamBody(Fs2Streams[F])(summon[Schema[List[OutputEvent[Hash]]]], CodecFormat.Json())
          .map {
            _.through(io.circe.fs2.byteArrayParser)
              .through(io.circe.fs2.decoder[F, OutputEvent[Hash]])
          } { events =>
            fs2
              .Stream
              .emit("[")
              .append(
                events
                  .map(_.asJson.noSpaces)
                  .intersperse(",")
              )
              .append(fs2.Stream.emit("]"))
              .through(fs2.text.utf8.encode)
          }
      )
      .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[Build.Error]))

  val run: PublicEndpoint[Hash, Nothing, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])

  val listImages: PublicEndpoint[Unit, Nothing, List[Hash], Any] = base
    .get
    .in("images")
    .out(jsonBody[List[Hash]])

}
