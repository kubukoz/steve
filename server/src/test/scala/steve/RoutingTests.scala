package steve

import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.dsl.io.*
import cats.implicits.*
import org.http4s.client.Client
import org.http4s.Method.*
import org.http4s.implicits.*
import io.circe.Json

class RoutingTests extends CatsEffectSuite {

  val exec = Client.fromHttpApp(
    Routing.instance(
      TestExecutor.instance(
        Map.empty,
        Map(Hash(Vector(40, 100)) -> SystemState(Map("K" -> "V")).asRight),
      )
    )
  )

  test("POST /api/run") {
    val input =
      io.circe
        .parser
        .parse("""
    {
      "value": [40, 100]
    }
    """).toOption
        .get

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
