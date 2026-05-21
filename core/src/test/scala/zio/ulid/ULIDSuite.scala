package zio.ulid

import munit.FunSuite

import scala.util.Random

class ULIDSuite extends FunSuite {

  private val rng = new Random(42)

  // ---- Value ----

  test("min ULID has all bits zero") {
    assertEquals(ULID.Min.msb, 0L)
    assertEquals(ULID.Min.lsb, 0L)
  }

  test("max ULID has all bits one") {
    assertEquals(ULID.Max.msb, -1L)
    assertEquals(ULID.Max.lsb, -1L)
  }

  test("time is extracted correctly from msb") {
    val time = 1_700_000_000_000L
    val ulid = ULID(time << 16, 0L)
    assertEquals(ulid.time, time)
  }

  test("instant matches time") {
    val time = java.lang.System.currentTimeMillis()
    val ulid = ULID(time << 16, 0L)
    assertEquals(ulid.instant.toEpochMilli, time)
  }

  test("minFor sets random bits to zero") {
    val time = 1_700_000_000_000L
    val ulid = ULID.minFor(time)
    assertEquals(ulid.time, time)
    assertEquals(ulid.lsb, 0L)
  }

  test("maxFor sets random bits to all one") {
    val time = 1_700_000_000_000L
    val ulid = ULID.maxFor(time)
    assertEquals(ulid.time, time)
    assertEquals(ulid.lsb, -1L)
  }

  test("increment on lsb carries into msb on overflow") {
    val ulid = new ULID(0x0000000000010000L, -1L)
    val next = ulid.increment
    assertEquals(next.lsb, 0L)
    assertEquals(next.msb, 0x0000000000010001L)
  }

  test("increment does not overflow msb under normal conditions") {
    val ulid = new ULID(1L, 0L)
    val next = ulid.increment
    assertEquals(next.lsb, 1L)
    assertEquals(next.msb, 1L)
  }

  test("apply(time, bytes) constructs correctly") {
    val time   = 1_700_000_000_000L
    val random = Array.fill[Byte](10)(0x42.toByte)
    val ulid   = ULID(time, random)
    assertEquals(ulid.time, time)
  }

  test("apply(time, bytes) rejects negative time") {
    assert(scala.util.Try(ULID(-1L, new Array[Byte](10))).isFailure)
  }

  test("apply(time, bytes) rejects wrong byte length") {
    assert(scala.util.Try(ULID(0L, new Array[Byte](9))).isFailure)
  }

  // ---- Encoding / decoding ----

  test("toString produces 26 characters") {
    assertEquals(ULID.fast().toString.length, 26)
  }

  test("toLowerCase produces 26 lowercase characters") {
    val s = ULID.fast().toLowerCase
    assertEquals(s.length, 26)
    assertEquals(s, s.toLowerCase)
  }

  test("fromString round-trips correctly") {
    repeat(200) {
      val msb  = rng.nextLong() & 0x0000ffffffffffffL
      val lsb  = rng.nextLong()
      val ulid = new ULID(msb, lsb)
      assertEquals(ULID.fromString(ulid.toString), ulid)
    }
  }

  test("fromString is case-insensitive") {
    val ulid  = ULID.fast()
    val upper = ulid.toString
    val lower = ulid.toLowerCase
    assertEquals(ULID.fromString(upper), ULID.fromString(lower))
  }

  test("fromString rejects string shorter than 26 chars") {
    assert(scala.util.Try(ULID.fromString("01ARZ3NDEKTSV4RRFFQ69G5FA")).isFailure)
  }

  test("fromString rejects string longer than 26 chars") {
    assert(scala.util.Try(ULID.fromString("01ARZ3NDEKTSV4RRFFQ69G5FAVX")).isFailure)
  }

  test("fromString rejects invalid characters") {
    assert(scala.util.Try(ULID.fromString("01ARZ3NDEKTSV4RRFFQ69G5F!!")).isFailure)
  }

  test("toBytes / fromBytes round-trips") {
    repeat(200) {
      val msb  = rng.nextLong() & 0x0000ffffffffffffL
      val lsb  = rng.nextLong()
      val ulid = new ULID(msb, lsb)
      assertEquals(ULID.fromBytes(ulid.toBytes), ulid)
    }
  }

  test("toUUID / fromUUID round-trips") {
    repeat(200) {
      val msb  = rng.nextLong() & 0x0000ffffffffffffL
      val lsb  = rng.nextLong()
      val ulid = new ULID(msb, lsb)
      assertEquals(ULID.fromUUID(ulid.toUUID), ulid)
    }
  }

  // ---- Validation ----

  test("isValid accepts valid ULID strings") {
    assert(ULID.isValid(ULID.fast().toString))
  }

  test("isValid rejects null") {
    assert(!ULID.isValid(null))
  }

  test("isValid rejects wrong length") {
    assert(!ULID.isValid("tooshort"))
  }

  test("isValid rejects overflow (first char > 7)") {
    assert(!ULID.isValid("8ZZZZZZZZZZZZZZZZZZZZZZZZZ"))
  }

  test("isValid accepts Crockford aliases O→0, I→1, L→1") {
    assert(ULID.isValid("O1ARZ3NDEKTSV4RRFFQ69G5FAV"))
  }

  // ---- Ordering ----

  test("ULIDs with larger timestamp sort after smaller ones") {
    assert(ULID.minFor(1000L) < ULID.minFor(2000L))
  }

  test("ULID.Min < ULID.Max") {
    assert(ULID.Min < ULID.Max)
  }

  test("equal ULIDs compare as 0") {
    val ulid = ULID.fast()
    assertEquals(ulid.compareTo(ulid), 0)
  }

  test("sorted list of ULIDs is lexicographically ordered") {
    repeat(50) {
      val times  = List.fill(10)(rng.nextLong() & 0x0000ffffffffffffL).sorted
      val ulids  = times.map(ULID.minFor)
      val sorted = ulids.sorted
      assertEquals(ulids, sorted)
    }
  }

  // ---- Conversion ----

  test("toRfc4122 sets version 4 bits") {
    assertEquals(ULID.fast().toRfc4122.toUUID.version(), 4)
  }

  test("toRfc4122 sets variant bits") {
    assertEquals(ULID.fast().toRfc4122.toUUID.variant(), 2)
  }

  private def repeat(n: Int)(f: => Unit): Unit =
    (1 to n).foreach(_ => f)
}
