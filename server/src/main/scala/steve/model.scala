package steve

final case class ResolvedBuild(
  base: SystemState,
  commands: List[ResolvedBuild.Command],
)

object ResolvedBuild {

  enum Command {
    case Upsert(key: String, value: String)
    case Delete(key: String)
  }

}
