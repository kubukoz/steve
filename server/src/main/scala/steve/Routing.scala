package steve

import cats.data.Kleisli
import cats.effect.kernel.Async
import cats.implicits.*
import org.http4s.HttpApp
import org.http4s.dsl.Http4sDsl
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.interceptor.ValuedEndpointOutput
import sttp.tapir.server.interceptor.exception.ExceptionHandler

object Routing {

  def instance[F[_]: Async](exec: Executor[F]): HttpApp[F] = {
    val endpoints: List[ServerEndpoint[Any, F]] = List(
      protocol.build.serverLogicRecoverErrors(exec.build),
      protocol.run.serverLogicSuccess(exec.run),
      protocol.listImages.serverLogicSuccess(_ => exec.listImages),
    )

    Http4sServerInterpreter[F](
      Http4sServerOptions
        .customInterceptors[F, F]
        .exceptionHandler { ex =>
          Some(
            ValuedEndpointOutput(
              jsonBody[GenericServerError].and(statusCode(StatusCode.InternalServerError)),
              GenericServerError("server failed"),
            )
          )
        }
        .options
    )
      .toRoutes(endpoints)
      .orNotFound
  }

}
