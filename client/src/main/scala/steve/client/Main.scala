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
import steve.client.FrontEnd.CLICommand
import sttp.tapir.client.http4s.Http4sClientInterpreter
import steve.Command
import steve.Executor
import steve.OutputEvent
import cats.MonadThrow
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends CommandIOApp("steve", "Command line interface for Steve") {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  def exec[F[_]: Async: Logger]: Resource[F, Executor[F]] = EmberClientBuilder
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

  def eval[F[_]: MonadThrow](exec: Executor[F])(using fs2.Compiler[F, F]): Command => F[String] = {
    case Command.Build(build) =>
      // todo: pretty-print stream failures
      // todo: pretty-print errors in stream
      // todo: pretty-print messages in stream
      OutputEvent
        .getResult(exec.build(build))
        .rethrow
        .map { hash =>
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
        // todo: enable with a flag?
        // steve.server.Main.serve.surround {
        exec[IO].use(eval[IO](_)(cmd))
        // }
      }
      .flatMap(IO.println(_))
      .as(ExitCode.Success)
  }

}

// object Example extends App {
//   println("┌ Starting build")
//   println(s"├ ${Console.BLUE}log 1${Console.RESET}")
//   println(s"${Console.YELLOW}├ Weird config${Console.RESET}")
//   println(s"├ ${Console.RED}Missing semicolon${Console.RESET}")
//   println(
//     s"└ ${Console.GREEN}Built image e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855${Console.RESET}"
//   )
// }
