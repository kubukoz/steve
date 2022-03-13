package steve

import fs2.io.file.Path
import fs2.io.file.Files
import cats.implicits.*
import cats.MonadThrow

trait BuildReader[F[_]] {
  def read(buildFile: Path): F[Build]
}

object BuildReader {
  def apply[F[_]](using F: BuildReader[F]): BuildReader[F] = F

  def instance[F[_]: Files: MonadThrow](using fs2.Compiler[F, F]): BuildReader[F] =
    buildFile =>
      Files[F]
        .readAll(buildFile)
        .through(fs2.text.utf8.decode[F])
        .compile
        .string
        .flatMap(io.circe.parser.decode[Build](_).liftTo[F])

}
