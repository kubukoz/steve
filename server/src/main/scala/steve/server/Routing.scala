package steve.server

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
import steve.Executor
import steve.protocol
import steve.GenericServerError
import sttp.capabilities.fs2.Fs2Streams
import org.typelevel.log4cats.Logger

object Routing {

  def instance[F[_]: Async: Logger](exec: Executor[F]): HttpApp[F] = {
    val endpoints: List[ServerEndpoint[Fs2Streams[F], F]] = List(
      protocol.build.serverLogicSuccess(exec.build(_).pure[F]),
      protocol.run.serverLogicRecoverErrors(exec.run),
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
