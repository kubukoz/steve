import steve.Hasher

import steve.SystemState

import steve.Hash

import steve.Build

import steve.Executor

import cats.effect.IO

import steve.ServerSideExecutor
import cats.effect.unsafe.implicits.*

val exec = ServerSideExecutor.module[IO].allocated.unsafeRunSync()._1

val build1Hash = exec
  .build(Build(Build.Base.EmptyImage, List(Build.Command.Upsert("hello", "value"))))
  .unsafeRunSync()

exec.run(build1Hash).unsafeRunSync()

val build2Hash = exec
  .build(
    Build(
      Build.Base.ImageReference(build1Hash),
      List(Build.Command.Upsert("hello2", "value")),
    )
  )
  .unsafeRunSync()

exec.run(build2Hash).unsafeRunSync()

exec
  .build(
    Build(
      Build.Base.ImageReference(build1Hash),
      List(Build.Command.Upsert("hello", "value"), Build.Command.Delete("hello")),
    )
  )
  .unsafeRunSync()

exec
  .build(
    Build(
      Build.Base.ImageReference(build1Hash),
      List(Build.Command.Upsert("hello", "value")),
    )
  )
  .unsafeRunSync()

exec.run(build2Hash).unsafeRunSync()
