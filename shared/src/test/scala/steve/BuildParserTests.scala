package steve

import weaver.*
import cats.kernel.Eq
import cats.implicits.*

object BuildParserTests extends FunSuite {

  extension (expect: Expect)(using SourceLocation)

    def parses(input: String, expected: Build): Expectations =
      BuildParser.parser.parseAll(input) match {
        case Left(err) =>
          def stripWhitespace(s: String) = s
            .replaceAll(" ", "·")
            .replaceAll("\n", "\\\\n\n")

          val (beforeOffset, afterOffset) = input
            .splitAt(err.failedAtOffset)
            .bimap(stripWhitespace, stripWhitespace)

          failure(
            s"""Parsing failed:\n$beforeOffset😡$afterOffset\n\nError: $err"""
          )
        case Right(result) => expect.eql(result, expected)
      }

    def brokenParse(input: String, offset: Int): Expectations = {
      val result = BuildParser.parser.parseAll(input)

      result match {
        case Left(e)  => assert.eql(e.failedAtOffset, offset)
        case Right(v) => failure(s"Parsing was expected to fail but didn't: $v")
      }
    }

  test("build with upsert command") {
    val input = """UPSERT hello world""".stripMargin

    assert.parses(
      input,
      Build(
        Build.Base.EmptyImage,
        List(
          Build
            .Command
            .Upsert(
              "hello",
              "world",
            )
        ),
      ),
    )
  }

  test("default build") {
    val input =
      """UPSERT hello world
        |DELETE hello""".stripMargin

    assert.parses(
      input,
      Build(
        Build.Base.EmptyImage,
        List(
          Build
            .Command
            .Upsert(
              "hello",
              "world",
            ),
          Build.Command.Delete("hello"),
        ),
      ),
    )
  }

  test("empty build") {
    assert.parses(
      "",
      Build.empty,
    )
  }

  test("empty build with a base") {
    assert.parses(
      "FROM e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      Build(
        Build
          .Base
          .ImageReference(
            Hash.unsafeParse("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
          ),
        List.empty,
      ),
    )
  }

  test("build with a base AND commands") {
    assert.parses(
      """FROM e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        |UPSERT foo bar
        |UPSERT foo2 baz""".stripMargin,
      Build(
        Build
          .Base
          .ImageReference(
            Hash.unsafeParse("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
          ),
        List(
          Build.Command.Upsert("foo", "bar"),
          Build.Command.Upsert("foo2", "baz"),
        ),
      ),
    )
  }

  test("build with a base AND commands") {
    assert.brokenParse(
      """FROM e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855UPSERT foo bar""".stripMargin,
      "FROM e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855".length,
    )
  }

  test("broken base") {
    assert.brokenParse(
      "USING base",
      0,
    )
  }

  test("base made of non-base64") {
    assert.brokenParse(
      "FROM 6208",
      "FROM 6208".length,
    )
  }

  test("base made of non-base64, case 2") {
    assert.brokenParse(
      "FROM h" + "a" * 63,
      "FROM ".length,
    )
  }
}
