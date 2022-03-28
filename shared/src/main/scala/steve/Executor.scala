package steve

import io.circe.Codec
import sttp.tapir.Schema

//move to model
enum OutputEvent[+A] derives Schema {
  case LogMessage(text: String)
  case Result(value: A)
}

object OutputEvent {
  given [A: Codec]: Codec.AsObject[OutputEvent[A]] = Codec.AsObject.derived
}

trait Executor[F[_]] {
  def build(build: Build): fs2.Stream[F, OutputEvent[Hash]]
  def run(hash: Hash): F[SystemState]
  def listImages: F[List[Hash]]
}

object Executor {
  def apply[F[_]](using F: Executor[F]): Executor[F] = F
}
