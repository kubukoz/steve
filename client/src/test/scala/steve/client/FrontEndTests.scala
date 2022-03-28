package steve.client

import weaver.*
import com.monovore.decline
import cats.implicits.*
import steve.client.FrontEnd.CLICommand
import cats.kernel.Eq
import com.monovore.decline.Help
import java.nio.file.Path
import java.nio.file.Paths
import steve.Hash

object FrontEndTests extends FunSuite {

  given Eq[Help] = Eq.fromUniversalEquals

  def parseCommand(
    args: String*
  ) = decline.Command("test", "Test command")(FrontEnd.parseInput).parse(args)

  test("build command") {
    assert.eql(
      parseCommand("build", "."),
      Right(CLICommand.Build(Paths.get("."))),
    )
  }

  test("run command") {
    val hashString = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    assert.eql(
      parseCommand("run", hashString),
      Right(CLICommand.Run(Hash.parse(hashString).toOption.get)),
    )
  }

  test("list command") {
    assert.eql(parseCommand("list"), Right(CLICommand.List))
  }
}
