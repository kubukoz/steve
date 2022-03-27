package steve.client

import cats.Applicative
import cats.Functor
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.Resource
import cats.effect.kernel.Async
import cats.implicits.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.io.file.Path
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import steve.client.FrontEnd.CLICommand
import sttp.tapir.client.http4s.Http4sClientInterpreter
import steve.Command
import steve.Executor

object Main extends CommandIOApp("steve", "Command line interface for Steve") {

  val logger = Slf4jLogger.getLogger[IO]

  def exec[F[_]: Async]: Resource[F, Executor[F]] = EmberClientBuilder
    .default[F]
    .build
    .map { client =>
      given Http4sClientInterpreter[F] = Http4sClientInterpreter[F]()

      ClientSideExecutor.instance[F](client)
    }

  def convertCommand[F[_]: BuildReader: Applicative]: CLICommand => F[Command] = {
    case CLICommand.Build(ctx) =>
      BuildReader[F]
        .read(
          Path.fromNioPath(ctx) / "steve.json"
        )
        .map(Command.Build(_))
    case CLICommand.Run(hash) => Command.Run(hash).pure[F]
    case CLICommand.List      => Command.ListImages.pure[F]
  }

  def eval[F[_]: Functor](exec: Executor[F]): Command => F[String] = {
    case Command.Build(build) =>
      exec.build(build).map { hash =>
        hash.toHex
      }

    case Command.Run(hash) =>
      exec.run(hash).map { state =>
        "System state:\n\n" + state.prettyPrint
      }

    case Command.ListImages =>
      exec.listImages.map { images =>
        images.mkString("\n")
      }
  }

  given BuildReader[IO] = BuildReader.instance

  val main: Opts[IO[ExitCode]] = FrontEnd.parseInput.map {
    convertCommand[IO](_)
      .flatMap { cmd =>
        exec[IO].use(eval[IO](_)(cmd))
      }
      .flatMap(IO.println(_))
      .as(ExitCode.Success)
  }

}
