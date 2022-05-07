package steve

import io.circe.Codec
import sttp.tapir.Schema
import cats.Show
import io.circe.Decoder
import io.circe.syntax.*
import cats.MonadError
import cats.kernel.Eq
import io.circe.Encoder

enum Command {
  case Build(build: steve.Build)
  case Run(hash: Hash)
  case ListImages
}

final case class Build(
  base: Build.Base,
  commands: List[Build.Command],
) derives Codec.AsObject,
    Schema

object Build {

  enum Base derives Codec.AsObject, Schema {
    case EmptyImage
    case ImageReference(hash: Hash)
  }

  enum Command derives Codec.AsObject, Schema {
    case Upsert(key: String, value: String)
    case Delete(key: String)
  }

  object Command {
    given Show[Command] = Show.fromToString
  }

  val empty: Build = Build(Base.EmptyImage, Nil)

  enum Error extends Exception derives Codec.AsObject, Schema {
    case UnknownBase(hash: Hash)
    case UnknownHash(hash: Hash)
  }

  object Error {
    given Eq[Error] = Eq.fromUniversalEquals

    given [A: Codec]: Codec[Either[Error, A]] = Codec.from(
      Decoder.decodeEither("error", "success"),
      Encoder.encodeEither("error", "success"),
    )

  }

}

enum RunError extends Exception derives Codec.AsObject, Schema {
  case UnknownHash(hash: Hash)
}

// todo customize schema
final case class Hash(value: Vector[Byte]) derives Schema {
  def toHex: String = value.map("%02X".format(_)).mkString.toLowerCase

  override def toString: String = toHex
}

object Hash {

  def parse(s: String): Either[String, Hash] =
    if (s.length % 2 == 0) {
      val bytes = s.grouped(2).map(Integer.parseInt(_, 16).toByte).toVector
      Right(Hash(bytes))
    } else {
      Left(s"Invalid hash: $s")
    }

  given Codec[Hash] = Codec.from(
    Decoder[String].emap(parse),
    _.toHex.asJson,
  )

  given Show[Hash] = Show.fromToString

  given Eq[Hash] = Eq.fromUniversalEquals
}

final case class SystemState(all: Map[String, String]) derives Codec.AsObject, Schema {
  def upsert(key: String, value: String): SystemState = SystemState(all + (key -> value))
  def delete(key: String): SystemState = SystemState(all - key)

  def prettyPrint: String = all
    .toList
    .sortBy(_._1)
    .map { case (key, value) => s"$key: $value" }
    .mkString("\n")

}

object SystemState {
  given Show[SystemState] = Show.fromToString
  given Eq[SystemState] = Eq.fromUniversalEquals

  val empty: SystemState = SystemState(Map.empty)
}

final case class GenericServerError(message: String) extends Exception
  derives Codec.AsObject,
    Schema

object GenericServerError {
  given Eq[GenericServerError] = Eq.fromUniversalEquals
}
