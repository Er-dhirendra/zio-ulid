package zio.ulid

import munit.FunSuite
import zio.Clock
import zio.ulid.test.ZioTestSupport

class ULIDGenSuite extends FunSuite {

  // ---- live (standard) ----

  test("live generates a valid ULID") {
    val ulid = ZioTestSupport.runWith(ULIDGen.live)(ULIDGen.generate)
    assert(ULID.isValid(ulid.toString))
  }

  test("live generates two different consecutive ULIDs") {
    val (a, b) = ZioTestSupport.runWith(ULIDGen.live) {
      for {
        a <- ULIDGen.generate
        b <- ULIDGen.generate
      } yield (a, b)
    }
    assertNotEquals(a, b)
  }

  test("live generateN produces exactly N ULIDs") {
    val ids = ZioTestSupport.runWith(ULIDGen.live)(ULIDGen.generateN(100))
    assertEquals(ids.length, 100)
  }

  test("live generated ULIDs have current timestamp") {
    val ulid = ZioTestSupport.runWith(ULIDGen.live) {
      for {
        before <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
        id     <- ULIDGen.generate
        after  <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)
      } yield (before, id, after)
    }
    assert(ulid._2.time >= ulid._1)
    assert(ulid._2.time <= ulid._3)
  }

  // ---- monotonic ----

  test("monotonic generates a valid ULID") {
    val ulid = ZioTestSupport.runWith(ULIDGen.monotonic)(ULIDGen.generate)
    assert(ULID.isValid(ulid.toString))
  }

  test("monotonic ULIDs generated in sequence are strictly ordered") {
    val ids = ZioTestSupport.runWith(ULIDGen.monotonic)(ULIDGen.generateN(1000))
    assert(ids.zip(ids.tail).forall { case (a, b) => a < b })
  }

  test("monotonic has no duplicates in 10000 generated") {
    val ids = ZioTestSupport.runWith(ULIDGen.monotonic)(ULIDGen.generateN(10000))
    assertEquals(ids.distinct.length, 10000)
  }

  test("monotonic increment overflow keeps time ahead of earlier wall-clock ms") {
    val nearMax = new ULID(0x0000000000010000L, -1L)
    val next    = nearMax.increment
    val earlier = ULID(next.time - 1, Array.fill[Byte](10)(0.toByte))
    assert(nearMax < next)
    assert(next > earlier)
  }

  // ---- fast ----

  test("fast generates a valid ULID") {
    val ulid = ZioTestSupport.runWith(ULIDGen.fast)(ULIDGen.generate)
    assert(ULID.isValid(ulid.toString))
  }

  test("fast generates distinct ULIDs") {
    val (a, b) = ZioTestSupport.runWith(ULIDGen.fast) {
      for {
        a <- ULIDGen.generate
        b <- ULIDGen.generate
      } yield (a, b)
    }
    assertNotEquals(a, b)
  }

  // ---- deterministic ----

  test("deterministic first ULID equals minFor(startTime)") {
    val startTime = 1_700_000_000_000L
    val ulid      = ZioTestSupport.runWith(ULIDGen.deterministic(startTime))(ULIDGen.generate)
    assertEquals(ulid, ULID.minFor(startTime))
  }

  test("deterministic each successive ULID increments by 1") {
    val ids = ZioTestSupport.runWith(ULIDGen.deterministic(0L))(ULIDGen.generateN(5))
    assert(ids.zip(ids.tail).forall { case (a, b) => b == a.increment })
  }

  test("deterministic output is reproducible") {
    val first  = ZioTestSupport.runWith(ULIDGen.deterministic(42L))(ULIDGen.generateN(3))
    val second = ZioTestSupport.runWith(ULIDGen.deterministic(42L))(ULIDGen.generateN(3))
    assertEquals(first, second)
  }
}
