package steve

import cats.implicits.*
import com.monovore.decline.Opts
import java.nio.file.Path

object FrontEnd {

  enum CLICommand {
    case Build(context: Path)
    case Run(hash: Hash)
    case List
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
