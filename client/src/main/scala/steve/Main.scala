package steve

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import cats.implicits.*
import com.monovore.decline.CommandApp
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.io.file.Files
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.client.http4s.Http4sClientInterpreter

import java.nio.file.Path

object Main extends CommandIOApp("steve", "Command line interface for Steve") {

  val input: Opts[IO[Command]] = {
    val build = Opts
      .subcommand("build", "Build an image")(
        Opts.argument[Path]("path")
      )
      .map { p =>
        Files[IO]
          .readAll(fs2.io.file.Path.fromNioPath(p) / "steve.json")
          .through(fs2.text.utf8.decode[IO])
          .compile
          .string
          .flatMap(io.circe.parser.decode[Build](_).liftTo[IO])
          .map(Command.Build(_))
      }

    val run =
      Opts
        .subcommand("run", "Run built image")(
          Opts
            .argument[String]("hash")
            .map(
              Hash
                .parse(_)
                .leftMap(new Exception(_))
                .liftTo[IO]
                .map(Command.Run(_))
            )
        )

    val list = Opts.subcommand("list", "List known images")(Opts(IO.pure(Command.ListImages)))

    build <+> run <+> list
  }

  val logger = Slf4jLogger.getLogger[IO]

  val exec: Resource[IO, Executor[IO]] = EmberClientBuilder
    .default[IO]
    .build
    .map { client =>
      given Http4sClientInterpreter[IO] = Http4sClientInterpreter[IO]()

      ClientSideExecutor.instance[IO](client)
    }

  def eval(exec: Executor[IO]): Command => IO[String] = {
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

  def main: Opts[IO[ExitCode]] = input.map {
    _.flatMap { cmd =>
      exec.use(eval(_)(cmd)).flatMap(IO.println(_))
    }.as(ExitCode.Success)
  }

}
