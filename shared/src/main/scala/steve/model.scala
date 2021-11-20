package steve

import io.circe.Codec
import sttp.tapir.Schema

enum Command {
  case Build(build: steve.Build)
  case Run(hash: Hash)
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

  val empty: Build = Build(Base.EmptyImage, Nil)

  enum Error extends Exception derives Codec.AsObject, Schema {
    case UnknownBase(hash: Hash)
  }

}

final case class Hash(value: Vector[Byte]) derives Codec.AsObject, Schema

final case class SystemState(all: Map[String, String]) derives Codec.AsObject, Schema

final case class GenericServerError(message: String) extends Exception
  derives Codec.AsObject,
    Schema
