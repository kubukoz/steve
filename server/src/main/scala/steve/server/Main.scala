package steve.server

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.host
import com.comcast.ip4s.port
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpApp

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    ServerSideExecutor
      .module[IO]
      .flatMap { exec =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp {
            Routing.instance[IO](exec)
          }
          .build
      }
      .useForever

}
