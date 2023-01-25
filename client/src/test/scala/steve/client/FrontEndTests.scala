package steve.client

import weaver.*
import com.monovore.decline
import cats.implicits.*
import steve.client.FrontEnd.CLICommand
import cats.kernel.Eq
import com.monovore.decline.Help
import java.nio.file.Paths
import steve.Hash
import steve.client.FrontEnd.CLIRun

object FrontEndTests extends FunSuite {

  given Eq[Help] = Eq.fromUniversalEquals

  def parseCommand(
    args: String*
  ) = decline.Command("test", "Test command")(FrontEnd.parseCLIRun).parse(args)

  test("build command") {
    assert.eql(
      parseCommand("build", "."),
      Right(CLIRun.remote(CLICommand.Build(Paths.get(".")))),
    )
  }

  test("run command") {
    val hashString = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    assert.eql(
      parseCommand("run", hashString),
      Right(CLIRun.remote(CLICommand.Run(Hash.parse(hashString).toOption.get))),
    )
  }

  test("list command") {
    assert.eql(parseCommand("list"), Right(CLIRun.remote(CLICommand.List)))
  }
}
