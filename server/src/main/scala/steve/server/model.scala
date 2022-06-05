package steve.server

import steve.SystemState
import cats.Show

final case class ResolvedBuild(
  base: SystemState,
  commands: List[ResolvedBuild.Command],
)

object ResolvedBuild {

  enum Command {
    case Upsert(key: String, value: String)
    case Delete(key: String)
  }

  given Show[Command] = Show.fromToString

}
