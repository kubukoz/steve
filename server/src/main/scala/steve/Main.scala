package steve

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.host
import com.comcast.ip4s.port
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp {

        val exec = ServerSideExecutor.instance[IO]

        val endpoints: List[ServerEndpoint[_, _, _, Any, IO]] = List(
          protocol.build.serverLogicInfallible(exec.build),
          protocol.run.serverLogicInfallible(exec.run),
        )

        Http4sServerInterpreter[IO]()
          .toRoutes(endpoints)
          .orNotFound
      }
      .build
      .useForever

}
