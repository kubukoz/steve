package steve.server

import cats.MonadThrow
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.implicits.*
import steve.SystemState
import steve.Hash

trait Registry[F[_]] {
  def save(system: SystemState): F[Hash]
  def lookup(hash: Hash): F[Option[SystemState]]
  def list: F[List[Hash]]
}

object Registry {
  def apply[F[_]](using F: Registry[F]): Registry[F] = F

  def instance[F[_]: MonadThrow: Ref.Make: Hasher]: F[Registry[F]] = inMemory[F]

  // todo: extract SystemStorage
  def inMemory[
    F[_]: MonadThrow: Ref.Make: Hasher
  ]: F[Registry[F]] = Ref[F].of(Map.empty[Hash, SystemState]).map { ref =>
    new Registry[F] {

      def save(system: SystemState): F[Hash] = Hasher[F].hash(system).flatMap { hash =>
        ref.modify { map =>
          (map + (hash -> system), hash)
        }
      }

      def lookup(hash: Hash): F[Option[SystemState]] = ref
        .get
        .map(_.get(hash))

      val list: F[List[Hash]] = ref.get.map(_.keys.toList)
    }
  }

}
