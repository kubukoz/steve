package steve

import io.circe.Codec
import sttp.tapir.Schema
import cats.Show
import io.circe.Decoder
import io.circe.syntax.*

sealed trait Command extends Product with Serializable

object Command {
  final case class Build(build: steve.Build) extends Command
  final case class Run(hash: Hash) extends Command
  final case class ListImages() extends Command
}

final case class Build(
  base: Build.Base,
  commands: List[Build.Command],
) derives Codec.AsObject,
    Schema

object Build {

  sealed trait Base extends Product with Serializable derives Codec.AsObject, Schema

  object Base {
    case object EmptyImage extends Base
    final case class ImageReference(hash: Hash) extends Base
  }

  sealed trait Command extends Product with Serializable derives Codec.AsObject, Schema

  object Command {
    final case class Upsert(key: String, value: String) extends Command
    final case class Delete(key: String) extends Command

    given Show[Command] = Show.fromToString
  }

  val empty: Build = Build(Base.EmptyImage, Nil)

  sealed trait Error extends Exception with Product with Serializable derives Codec.AsObject, Schema

  object Error {
    final case class UnknownBase(hash: Hash) extends Error
    final case class UnknownHash(hash: Hash) extends Error
  }

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
  // todo custom codec
}

final case class SystemState(all: Map[String, String]) derives Codec.AsObject, Schema {
  def upsert(key: String, value: String): SystemState = SystemState(all + (key -> value))
  def delete(key: String): SystemState = SystemState(all - key)
}

object SystemState {
  given Show[SystemState] = Show.fromToString

  val empty: SystemState = SystemState(Map.empty)
}

final case class GenericServerError(message: String) extends Exception
  derives Codec.AsObject,
    Schema
