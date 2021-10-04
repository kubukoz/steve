package steve

import cats.effect.kernel.Async
import cats.implicits.*
import org.http4s.HttpApp
import org.http4s.Response
import org.http4s.Status
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.interceptor.ValuedEndpointOutput
import sttp.tapir.server.interceptor.exception.ExceptionInterceptor
import sttp.model.StatusCode

object Routing {

  def instance[F[_]: Async](exec: Executor[F]): HttpApp[F] = {
    import org.http4s.implicits._

    val endpoints: List[ServerEndpoint[_, _, _, Any, F]] = List(
      protocol.build.serverLogicRecoverErrors(exec.build),
      protocol.run.serverLogicInfallible(exec.run),
    )

    Http4sServerInterpreter[F](
      Http4sServerOptions
        .customInterceptors[F, F]
        .exceptionHandler { _ =>
          ValuedEndpointOutput(
            jsonBody[GenericServerError].and(sttp.tapir.statusCode(StatusCode.InternalServerError)),
            GenericServerError("unhandled exception"),
          ).some
        }
        .options
    )
      .toRoutes(endpoints)
      .orNotFound
  }

}
