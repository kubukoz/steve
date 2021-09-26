package steve

import cats.effect.IOApp
import cats.effect.IO
import cats.implicits.*
import org.http4s.ember.client.EmberClientBuilder
import sttp.tapir.client.http4s.Http4sClientInterpreter
import org.http4s.client.Client
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple {

  val logger = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] = EmberClientBuilder
    .default[IO]
    .build
    .use { client =>

      given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

      val exec = ClientSideExecutor
        .instance[IO](client)

      //todo: better logging
      logger.info("Building base image") *>
        exec
          .build(Build.empty)
          .flatTap(hash => logger.info("Built image with hash: " + hash))
          .flatMap(exec.run)
          .flatMap(result => logger.info("Ran image with result: " + result))
    }
    .orElse(logger.error("Unhandled error"))

}
