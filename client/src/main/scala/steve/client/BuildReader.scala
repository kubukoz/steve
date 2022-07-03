package steve.client

import fs2.io.file.Path
import fs2.io.file.Files
import cats.implicits.*
import cats.effect.Concurrent
import steve.Build
import steve.BuildParser
import cats.parse.Parser

trait BuildReader[F[_]] {
  def read(buildFile: Path): F[Build]
}

object BuildReader {
  def apply[F[_]](using F: BuildReader[F]): BuildReader[F] = F

  def instance[F[_]: Files: Concurrent]: BuildReader[F] =
    buildFile =>
      Files[F]
        .readAll(buildFile)
        .through(fs2.text.utf8.decode[F])
        .compile
        .string
        .flatMap(
          BuildParser.parser.parseAll(_).leftMap(ParsingFailure(_)).liftTo[F]
        )

  final case class ParsingFailure(e: Parser.Error) extends Exception(e.toString())
}
