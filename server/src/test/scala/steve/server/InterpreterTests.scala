package steve.server

import cats.Id
import cats.catsInstancesForId
import weaver.*
import weaver.scalacheck.Checkers
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import ResolvedBuild.Command.*
import Arbitraries.given
import steve.SystemState
import cats.effect.IO
import cats.implicits.*
import steve.OutputEvent

object InterpreterTests extends SimpleIOSuite with Checkers {
  val interpreter = Interpreter.instance[IO]

  private def interpretResult(build: ResolvedBuild) =
    OutputEvent
      .getResult(interpreter.interpret(build))
      .rethrow

  test("Upsert emits event") {

    forall { (system: SystemState) =>
      interpreter
        .interpret(ResolvedBuild(system, List(Upsert("k", "v"))))
        .compile
        .toList
        .map { events =>
          assert(events.contains(OutputEvent.LogMessage("Upserting k: v")))
        }

    }

  }

  test("Delete emits event") {
    forall { (system: SystemState) =>
      interpreter
        .interpret(ResolvedBuild(system, List(Delete("k"))))
        .compile
        .toList
        .map { events =>
          assert(events.contains(OutputEvent.LogMessage("Deleting k")))
        }
    }
  }

  test("Multiple commands emit multiple events") {

    forall { (system: SystemState) =>
      interpreter
        .interpret(ResolvedBuild(system, List(Upsert("k", "v"), Delete("k"))))
        .compile
        .toList
        .map { events =>
          val expected = List(
            "Upserting k: v",
            "Deleting k",
          )

          val actual = events.collect { case OutputEvent.LogMessage(msg) =>
            msg
          }

          assert.eql(actual, expected)
        }

    }

  }

  test("any system + upsert => the key in the system has the given value") {
    forall { (system: SystemState, key: String, value: String) =>
      val build = ResolvedBuild(system, List(Upsert(key, value)))

      interpretResult(build)
        .map { state =>
          assert.eql(
            state.all.get(key),
            Some(value),
          )

        }
    }
  }

  test("any system + delete => the key is missing") {
    forall { (system: SystemState, key: String) =>
      val build = ResolvedBuild(system, List(Delete(key)))

      interpretResult(build)
        .map { state =>
          assert.eql(
            state.all.get(key),
            None,
          )
        }
    }
  }

  test("upsert(k, v) + delete(k) == delete(k)") {
    forall { (system: SystemState, key: String, value: String) =>
      val build = ResolvedBuild(system, List(Upsert(key, value), Delete(key)))
      val build2 = ResolvedBuild(system, List(Delete(key)))

      (build, build2)
        .bitraverse(interpretResult, interpretResult)
        .map(assert.eql(_, _))
    }
  }

  test("upsert(k, v1) + upsert(k, v2) == upsert(k, v2)") {
    forall { (system: SystemState, key: String, value1: String, value2: String) =>
      val build = ResolvedBuild(system, List(Upsert(key, value1), Upsert(key, value2)))
      val build2 = ResolvedBuild(system, List(Upsert(key, value2)))

      (build, build2)
        .bitraverse(interpretResult, interpretResult)
        .map(assert.eql(_, _))
    }
  }

  test("split build results in the same state as a combined build") {
    forall {
      (
        system: SystemState,
        commands: List[ResolvedBuild.Command],
        moreCommands: List[ResolvedBuild.Command],
      ) =>
        val lhs = interpretResult(ResolvedBuild(system, commands)).flatMap { sys1 =>
          interpretResult(ResolvedBuild(sys1, moreCommands))
        }

        val rhs = interpretResult(ResolvedBuild(system, commands ++ moreCommands))

        (lhs, rhs)
          .mapN(assert.eql(_, _))
    }
  }

}
