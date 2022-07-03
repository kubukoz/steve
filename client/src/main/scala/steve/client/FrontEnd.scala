package steve.client

import cats.implicits.*
import com.monovore.decline.Opts
import java.nio.file.Path
import cats.kernel.Eq
import steve.Hash

object FrontEnd {

  final case class CLIRun(
    command: CLICommand,
    options: CLIOptions,
  )

  object CLIRun {
    def remote(command: CLICommand): CLIRun = CLIRun(command, CLIOptions(standalone = false))

    given Eq[CLIRun] = Eq.fromUniversalEquals
  }

  enum CLICommand {
    case Build(context: Path)
    case Run(hash: Hash)
    case List
  }

  object CLICommand {
    given Eq[CLICommand] = Eq.fromUniversalEquals
  }

  final case class CLIOptions(standalone: Boolean)

  private val parseCLICommand: Opts[CLICommand] = {
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

  val parseCLIRun: Opts[CLIRun] =
    (
      parseCLICommand,
      Opts.flag("standalone", "Run without server").orFalse.map(CLIOptions(_)),
    ).mapN(CLIRun.apply)

}
