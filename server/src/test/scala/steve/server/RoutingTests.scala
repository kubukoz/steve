package steve.server

import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.dsl.io.*
import cats.implicits.*
import org.http4s.client.Client
import org.http4s.Method.*
import org.http4s.implicits.*
import io.circe.Json
import steve.TestExecutor
import steve.Hash
import steve.SystemState
import org.typelevel.log4cats.Logger
import cats.effect.IO
import org.typelevel.log4cats.noop.NoOpLogger
import steve.TestExecutor.TestResult

class RoutingTests extends CatsEffectSuite {
  given Logger[IO] = NoOpLogger[IO]

  val exec = Client.fromHttpApp(
    Routing.instance(
      TestExecutor.instance(
        Map.empty,
        Map(Hash(Vector(40, 100)) -> TestResult.Success(SystemState(Map("K" -> "V")))),
      )
    )
  )

  test("POST /api/run") {
    val input = Json.fromString("2864")

    val output =
      io.circe
        .parser
        .parse("""
      {
        "all": {
          "K": "V"
        }
      }
    """).toOption
        .get

    assertIO(
      exec.expect[Json](
        POST(input, uri"/api/run")
      ),
      output,
    )
  }
}
