package steve

import org.http4s.HttpApp
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.kernel.Async

object Routing {

  def instance[F[_]: Async](exec: Executor[F]): HttpApp[F] = {
    val endpoints: List[ServerEndpoint[_, _, _, Any, F]] = List(
      protocol.build.serverLogicInfallible(exec.build),
      protocol.run.serverLogicInfallible(exec.run),
    )

    Http4sServerInterpreter[F]()
      .toRoutes(endpoints)
      .orNotFound
  }

}
