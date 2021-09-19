package steve

import sttp.tapir.Endpoint
import io.circe.Codec as CirceCodec

object protocol {
  import sttp.tapir._
  import sttp.tapir.json.circe._

  private val base = infallibleEndpoint.in("api")

  val build: Endpoint[Build, Nothing, Hash, Any] = base
    .put
    .in("build")
    .in(jsonBody[Build])
    .out(jsonBody[Hash])

  val run: Endpoint[Hash, Nothing, SystemState, Any] = base
    .post
    .in("run")
    .in(jsonBody[Hash])
    .out(jsonBody[SystemState])

}
