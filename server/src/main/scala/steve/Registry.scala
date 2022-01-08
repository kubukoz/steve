package steve

import cats.MonadThrow
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.UUIDGen
import cats.implicits.*

trait Registry[F[_]] {
  def save(system: SystemState): F[Hash]
  def lookup(hash: Hash): F[Option[SystemState]]
}

object Registry {
  def apply[F[_]](using F: Registry[F]): Registry[F] = F

  val emptyHash: Hash = Hash(Vector.empty)

  def inMemory[F[_]: MonadThrow: Ref.Make: UUIDGen]: Resource[F, Registry[F]] = {
    val emptySystem: SystemState = SystemState(Map.empty)

    Ref[F].of(Map(emptyHash -> emptySystem)).toResource.map { ref =>
      new Registry[F] {

        def save(system: SystemState): F[Hash] = UUIDGen[F].randomUUID.flatMap { uuid =>
          ref.modify { map =>
            val hash = map
              .collectFirst { case (k, `system`) => k }
              .getOrElse(Hash(uuid.toString().getBytes.toVector))

            (map + (hash -> system), hash)
          }
        }

        def lookup(hash: Hash): F[Option[SystemState]] = ref
          .get
          .map(_.get(hash))
      }
    }
  }

}
