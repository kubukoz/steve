package steve

import cats.MonadThrow
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.implicits.*

trait Registry[F[_]] {
  def save(system: SystemState): F[Hash]
  def lookup(hash: Hash): F[Option[SystemState]]
}

object Registry {
  def apply[F[_]](using F: Registry[F]): Registry[F] = F

  val emptyHash: Hash = Hash(Vector.empty)
  val emptySystem: SystemState = SystemState(Map.empty)

  def instance[F[_]: MonadThrow: Ref.Make: Hasher]: Resource[F, Registry[F]] = inMemory(
    Map(emptyHash -> emptySystem)
  )

  def inMemory[F[_]: MonadThrow: Ref.Make: Hasher](
    initialState: Map[Hash, SystemState]
  ): Resource[F, Registry[F]] = Ref[F].of(initialState).toResource.map { ref =>
    new Registry[F] {

      def save(system: SystemState): F[Hash] = Hasher[F].hash(system).flatMap { hash =>
        ref.modify { map =>
          (map + (hash -> system), hash)
        }
      }

      def lookup(hash: Hash): F[Option[SystemState]] = ref
        .get
        .map(_.get(hash))
    }
  }

}
