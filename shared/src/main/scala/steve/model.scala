package steve

import io.circe.Codec
import sttp.tapir.Schema

sealed trait Command extends Product with Serializable

object Command {
  final case class Build(build: steve.Build) extends Command
  final case class Run(hash: Hash) extends Command
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
  }

  val empty: Build = Build(Base.EmptyImage, Nil)
}

final case class Hash(value: Array[Byte]) derives Codec.AsObject, Schema

final case class SystemState(all: Map[String, String]) derives Codec.AsObject, Schema
