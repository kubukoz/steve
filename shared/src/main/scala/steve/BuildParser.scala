package steve

import cats.parse.Parser
import cats.parse.Parser as P
import cats.implicits.*
import cats.parse.Parser0 as P0
import cats.parse.Rfc5234
import cats.parse.Numbers

object BuildParser {

  val parser: P0[Build] = {
    val whitespace: P[Unit] = P.charIn(" \t\n").void

    val command: P[Build.Command] = {

      val key: P0[String] = (Rfc5234.alpha | Numbers.digit).rep0.string
      val value: P0[String] = P.until(whitespace)

      val upsert: P[Build.Command.Upsert] =
        (
          P.string("UPSERT") *> P.char(' ') *> key,
          P.char(' ') *> value,
        ).mapN(Build.Command.Upsert.apply)

      val delete: P[Build.Command.Delete] =
        whitespace.rep0.with1 *>
          P.string("DELETE") *>
          P.char(' ') *>
          key.map(Build.Command.Delete(_))

      upsert | delete
    }

    val base: P0[Build.Base] = {

      val hash: P[Hash] = P
        .charIn(('0' to '9') ++ ('a' to 'f'))
        .rep(64)
        .string
        .map(Hash.unsafeParse)

      (
        P.string("FROM") *>
          P.char(' ') *>
          hash.map(Build.Base.ImageReference(_)) <*
          (P.char('\n') | P.end)
      )
        .orElse(P.pure(Build.Base.EmptyImage))
    }

    val commands: P0[List[Build.Command]] = command
      .repSep0(P.char('\n'))

    (base, commands).mapN(Build.apply)
  }

}
