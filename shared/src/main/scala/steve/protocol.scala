package steve

import sttp.tapir.Endpoint
import io.circe.Codec as CirceCodec
import sttp.model.StatusCodes
import sttp.model.StatusCode

object protocol {
  import sttp.tapir._
  import sttp.tapir.json.circe._

  private val base = endpoint
    .in("api")
    .errorOut(jsonBody[GenericServerError].and(statusCode(StatusCode.InternalServerError)))

  val build: Endpoint[Build, GenericServerError, Hash, Any] = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(jsonBody[Hash])

  val run: Endpoint[Hash, GenericServerError, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])

}
