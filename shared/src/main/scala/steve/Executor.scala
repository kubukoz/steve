package steve

trait Executor[F[_]] {
  def build(build: Build): F[Hash]
  def run(hash: Hash): F[SystemState]
}

object Executor {
  def apply[F[_]](using F: Executor[F]): Executor[F] = F
}
