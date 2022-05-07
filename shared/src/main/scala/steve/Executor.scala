package steve

import io.circe.Codec
import sttp.tapir.Schema
import cats.MonadThrow
import cats.Eq
import cats.implicits.*
import cats.Functor

//move to model
enum OutputEvent[+A] derives Schema {
  case LogMessage(text: String)
  case Result(value: A)
  case Failure(e: GenericServerError)
}

object OutputEvent {
  given [A: Codec]: Codec.AsObject[OutputEvent[A]] = Codec.AsObject.derived

  given [A: Eq]: Eq[OutputEvent[A]] = {
    case (LogMessage(a), LogMessage(b)) => a === b
    case (Result(a), Result(b))         => a === b
    case (Failure(a), Failure(b))       => a === b
    case _                              => false
  }

  given Functor[OutputEvent] =
    new Functor[OutputEvent] {

      def map[A, B](fa: OutputEvent[A])(f: A => B): OutputEvent[B] =
        fa match {
          case Result(a)       => Result(f(a))
          case Failure(a)      => Failure(a)
          case LogMessage(msg) => LogMessage(msg)
        }

    }

  def getResult[F[_]: MonadThrow, A](
    stream: fs2.Stream[F, OutputEvent[A]]
  )(
    using fs2.Compiler[F, F]
  ): F[A] =
    stream
      .collectFirst { case Result(a) => a }
      .compile
      .lastOrError

}

trait Executor[F[_]] {
  def build(build: Build): fs2.Stream[F, OutputEvent[Either[Build.Error, Hash]]]
  def run(hash: Hash): F[SystemState]
  def listImages: F[List[Hash]]
}

object Executor {
  def apply[F[_]](using F: Executor[F]): Executor[F] = F
}
