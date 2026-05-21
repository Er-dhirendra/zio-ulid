package zio.ulid.test

import zio.{Runtime, Unsafe, ZIO, ZLayer}

/** Runs pure ZIO effects from MUnit tests. */
object ZioTestSupport {

  def run[A](effect: ZIO[Any, Nothing, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(effect).getOrThrow()
    }

  def runWith[R, A](layer: ZLayer[Any, Nothing, R])(effect: ZIO[R, Nothing, A]): A =
    run(effect.provide(layer))
}
