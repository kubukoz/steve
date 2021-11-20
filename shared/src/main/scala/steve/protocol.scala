package steve

import sttp.tapir.Endpoint
import io.circe.Codec as CirceCodec
import sttp.model.StatusCode

object protocol {
  import sttp.tapir._
  import sttp.tapir.json.circe._

  private val base = infallibleEndpoint.in("api")

  val build: PublicEndpoint[Build, Build.Error, Hash, Any] = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(jsonBody[Hash])
    .errorOut(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[Build.Error]))

  val run: PublicEndpoint[Hash, Nothing, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])

}
