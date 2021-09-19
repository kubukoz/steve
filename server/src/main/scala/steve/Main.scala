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

        val endpoints: List[ServerEndpoint[_, _, _, Any, IO]] = List(
          protocol.build.serverLogicInfallible { build =>
            IO.println(build).as(Hash(Array()))
          },
          protocol.run.serverLogicInfallible { hash =>
            IO.println(hash).as(SystemState(Map.empty))
          },
        )

        Http4sServerInterpreter[IO]()
          .toRoutes(endpoints)
          .orNotFound
      }
      .build
      .useForever

}
