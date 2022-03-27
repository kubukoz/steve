package steve.server

import weaver.*
import cats.effect.IO
import steve.SystemState

object HasherTests extends SimpleIOSuite {

  val hasher = Hasher.sha256Hasher[IO]

  test("empty hash") {
    hasher.hash(SystemState.empty).map { result =>
      assert(result.toHex == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }
  }

  test("hash with one element") {
    hasher.hash(SystemState.empty.upsert("1", "a")).map { result =>
      assert(result.toHex == "4162fddd39a3e4225e8e2392eced237fbeb34e6e218b5647d27bd4d2b9c0da24")
    }
  }

  test("hash with two elements") {
    hasher.hash(SystemState.empty.upsert("1", "a").upsert("2", "b")).map { result =>
      assert(result.toHex == "10c0ac6fa81a940d7cffe36ed095275f338f529f1ff8af912db9f13321e530d6")
    }
  }

  test("insert + delete is empty hash") {
    hasher.hash(SystemState.empty.upsert("1", "a").delete("1")).map { result =>
      assert(result.toHex == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }
  }
}
