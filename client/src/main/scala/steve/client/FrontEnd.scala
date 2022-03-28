package steve.client

import cats.implicits.*
import com.monovore.decline.Opts
import java.nio.file.Path
import cats.kernel.Eq
import steve.Hash

object FrontEnd {

  enum CLICommand {
    case Build(context: Path)
    case Run(hash: Hash)
    case List
  }

  object CLICommand {
    given Eq[CLICommand] = Eq.fromUniversalEquals
  }

  val parseInput: Opts[CLICommand] = {
    val build = Opts
      .subcommand("build", "Build an image")(
        Opts.argument[Path]("path")
      )
      .map(CLICommand.Build(_))

    val run =
      Opts
        .subcommand("run", "Run built image")(
          Opts
            .argument[String]("hash")
            .mapValidated(Hash.parse(_).toValidatedNel)
            .map(CLICommand.Run(_))
        )

    val list = Opts.subcommand("list", "List known images")(Opts(CLICommand.List))

    build <+> run <+> list
  }

}
