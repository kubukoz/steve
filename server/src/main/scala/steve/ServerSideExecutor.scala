package steve

import cats.implicits.*
import cats.ApplicativeThrow

object ServerSideExecutor {

  def instance[F[_]: ApplicativeThrow]: Executor[F] =
    new Executor[F] {
      private val emptyHash: Hash = Hash(Vector.empty)

      def build(build: Build): F[Hash] = (build == Build.empty)
        .guard[Option]
        .as(emptyHash)
        .liftTo[F](GenericServerError("Unsupported build!"))

      def run(hash: Hash): F[SystemState] = (hash == emptyHash)
        .guard[Option]
        .as(SystemState(Map.empty))
        .liftTo[F](GenericServerError("Unsupported hash!"))

    }

}
