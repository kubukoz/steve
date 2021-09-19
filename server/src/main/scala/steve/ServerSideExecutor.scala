package steve

import cats.implicits.*
import cats.ApplicativeThrow

object ServerSideExecutor {

  def instance[F[_]: ApplicativeThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Array())

      def build(build: Build): F[Hash] = (build == Build.empty)
        .guard[Option]
        .as(emptyHash)
        .liftTo[F](new Throwable("Unsupported build!"))

      def run(hash: Hash): F[SystemState] = (hash == emptyHash)
        .guard[Option]
        .as(KVState(Map.empty))
        .liftTo[F](new Throwable("Unsupported hash!"))

    }

  private final case class KVState(getAll: Map[String, String]) extends SystemState

}
