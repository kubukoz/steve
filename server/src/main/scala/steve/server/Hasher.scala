package steve.server

import cats.effect.kernel.Sync
import cats.implicits.*
import java.security.MessageDigest
import cats.Functor
import steve.SystemState
import steve.Hash

trait Hasher[F[_]] {
  def hash(system: SystemState): F[Hash]
}

object Hasher {
  def apply[F[_]](using F: Hasher[F]): Hasher[F] = F

  def sha256Hasher[F[_]: Sync]: Hasher[F] =
    system =>
      val systemBytes: Array[Byte] =
        system.all.toList.sortBy(_._1).map { case (k, v) => s"$k:$v" }.mkString.getBytes

      Sync[F]
        .delay {
          MessageDigest
            .getInstance("SHA-256")
            .digest(systemBytes)
        }
        .map(bytes => Hash(bytes.toVector))

}
