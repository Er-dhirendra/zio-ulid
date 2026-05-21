package zio.ulid

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * A ULID (Universally Unique Lexicographically Sortable Identifier).
 *
 * A ULID is a 128-bit value encoded as a 26-character Crockford Base32 string.
 * It is composed of:
 *   - 48 bits of millisecond timestamp (sortable, unique per ms)
 *   - 80 bits of randomness
 *
 * ULIDs are:
 *   - Lexicographically sortable
 *   - URL-safe (no special characters)
 *   - Case-insensitive
 *   - 128-bit compatible with UUID
 *
 * Example: `01ARZ3NDEKTSV4RRFFQ69G5FAV`
 *
 * @see [[https://github.com/ulid/spec ULID Specification]]
 */
final class ULID private[ulid] (val msb: Long, val lsb: Long)
    extends Ordered[ULID]
    with Serializable {

  /** The 48-bit time component in milliseconds since Unix epoch. */
  def time: Long = msb >>> 16

  /** The time component as a [[java.time.Instant]]. */
  def instant: Instant = Instant.ofEpochMilli(time)

  /** The 10-byte random component. */
  def randomBytes: Array[Byte] = {
    val bytes = new Array[Byte](ULID.RandomBytes)
    bytes(0) = (msb >>> 8).toByte
    bytes(1) = msb.toByte
    bytes(2) = (lsb >>> 56).toByte
    bytes(3) = (lsb >>> 48).toByte
    bytes(4) = (lsb >>> 40).toByte
    bytes(5) = (lsb >>> 32).toByte
    bytes(6) = (lsb >>> 24).toByte
    bytes(7) = (lsb >>> 16).toByte
    bytes(8) = (lsb >>> 8).toByte
    bytes(9) = lsb.toByte
    bytes
  }

  /** Converts this ULID to a 16-byte array. */
  def toBytes: Array[Byte] = {
    val bytes = new Array[Byte](ULID.Bytes)
    bytes(0)  = (msb >>> 56).toByte
    bytes(1)  = (msb >>> 48).toByte
    bytes(2)  = (msb >>> 40).toByte
    bytes(3)  = (msb >>> 32).toByte
    bytes(4)  = (msb >>> 24).toByte
    bytes(5)  = (msb >>> 16).toByte
    bytes(6)  = (msb >>> 8).toByte
    bytes(7)  = msb.toByte
    bytes(8)  = (lsb >>> 56).toByte
    bytes(9)  = (lsb >>> 48).toByte
    bytes(10) = (lsb >>> 40).toByte
    bytes(11) = (lsb >>> 32).toByte
    bytes(12) = (lsb >>> 24).toByte
    bytes(13) = (lsb >>> 16).toByte
    bytes(14) = (lsb >>> 8).toByte
    bytes(15) = lsb.toByte
    bytes
  }

  /** Converts this ULID to a [[java.util.UUID]] (128-bit compatible). */
  def toUUID: UUID = new UUID(msb, lsb)

  /**
   * Converts this ULID to a RFC-4122 UUIDv4-compatible value.
   * Note: this changes 6 bits and is not reversible.
   */
  def toRfc4122: ULID = {
    val msb4 = (msb & 0xffffffffffff0fffL) | 0x0000000000004000L // version 4
    val lsb4 = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L // variant 10
    new ULID(msb4, lsb4)
  }

  /**
   * Returns a new ULID with the random component incremented by 1.
   * Used internally for monotonic generation.
   * If the random component overflows, the time bits are incremented
   * to maintain monotonicity.
   */
  def increment: ULID = {
    val newLsb = lsb + 1L
    val newMsb = if (newLsb == 0L) msb + 1L else msb
    new ULID(newMsb, newLsb)
  }

  /** Encodes this ULID as a 26-character uppercase Crockford Base32 string. */
  override def toString: String = encode(ULID.AlphabetUpper)

  /** Encodes this ULID as a 26-character lowercase Crockford Base32 string. */
  def toLowerCase: String = encode(ULID.AlphabetLower)

  private def encode(alphabet: Array[Char]): String = {
    val chars   = new Array[Char](ULID.StringChars)
    val time    = msb >>> 16
    val rand0   = ((msb & 0xffffL) << 24) | (lsb >>> 40)
    val rand1   = lsb & 0xffffffffffL

    chars(0)  = alphabet(((time  >>> 45) & 0x1f).toInt)
    chars(1)  = alphabet(((time  >>> 40) & 0x1f).toInt)
    chars(2)  = alphabet(((time  >>> 35) & 0x1f).toInt)
    chars(3)  = alphabet(((time  >>> 30) & 0x1f).toInt)
    chars(4)  = alphabet(((time  >>> 25) & 0x1f).toInt)
    chars(5)  = alphabet(((time  >>> 20) & 0x1f).toInt)
    chars(6)  = alphabet(((time  >>> 15) & 0x1f).toInt)
    chars(7)  = alphabet(((time  >>> 10) & 0x1f).toInt)
    chars(8)  = alphabet(((time  >>>  5) & 0x1f).toInt)
    chars(9)  = alphabet((time           & 0x1f).toInt)
    chars(10) = alphabet(((rand0 >>> 35) & 0x1f).toInt)
    chars(11) = alphabet(((rand0 >>> 30) & 0x1f).toInt)
    chars(12) = alphabet(((rand0 >>> 25) & 0x1f).toInt)
    chars(13) = alphabet(((rand0 >>> 20) & 0x1f).toInt)
    chars(14) = alphabet(((rand0 >>> 15) & 0x1f).toInt)
    chars(15) = alphabet(((rand0 >>> 10) & 0x1f).toInt)
    chars(16) = alphabet(((rand0 >>>  5) & 0x1f).toInt)
    chars(17) = alphabet((rand0          & 0x1f).toInt)
    chars(18) = alphabet(((rand1 >>> 35) & 0x1f).toInt)
    chars(19) = alphabet(((rand1 >>> 30) & 0x1f).toInt)
    chars(20) = alphabet(((rand1 >>> 25) & 0x1f).toInt)
    chars(21) = alphabet(((rand1 >>> 20) & 0x1f).toInt)
    chars(22) = alphabet(((rand1 >>> 15) & 0x1f).toInt)
    chars(23) = alphabet(((rand1 >>> 10) & 0x1f).toInt)
    chars(24) = alphabet(((rand1 >>>  5) & 0x1f).toInt)
    chars(25) = alphabet((rand1          & 0x1f).toInt)
    new String(chars)
  }

  /** Unsigned 128-bit comparison. */
  override def compare(that: ULID): Int = {
    val min  = Long.MinValue
    val a    = msb + min
    val b    = that.msb + min
    if      (a > b) 1
    else if (a < b) -1
    else {
      val c = lsb + min
      val d = that.lsb + min
      if      (c > d) 1
      else if (c < d) -1
      else            0
    }
  }

  override def equals(obj: Any): Boolean =
    if (obj.asInstanceOf[AnyRef] eq this) true
    else if (!obj.isInstanceOf[ULID]) false
    else {
      val that = obj.asInstanceOf[ULID]
      msb == that.msb && lsb == that.lsb
    }

  override def hashCode: Int = {
    val bits = msb ^ lsb
    (bits ^ (bits >>> 32)).toInt
  }
}

object ULID {

  // ---- Dimensions ----
  val StringChars = 26
  val TimeChars   = 10
  val RandomChars = 16
  val Bytes       = 16
  val TimeBytes   = 6
  val RandomBytes = 10

  // Crockford Base32: excludes I, L, O, U to avoid visual confusion
  val AlphabetUpper: Array[Char] = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray
  val AlphabetLower: Array[Char] = "0123456789abcdefghjkmnpqrstvwxyz".toCharArray

  // Reverse lookup: char code → 5-bit value (-1 = invalid)
  val CharValues: Array[Byte] = {
    val v = Array.fill[Byte](256)(-1)
    AlphabetUpper.zipWithIndex.foreach { case (c, i) => v(c.toInt) = i.toByte }
    AlphabetLower.zipWithIndex.foreach { case (c, i) => v(c.toInt) = i.toByte }
    // Crockford-specified aliases
    v('O') = 0; v('o') = 0
    v('I') = 1; v('i') = 1
    v('L') = 1; v('l') = 1
    v
  }

  /** The minimum possible ULID (all bits zero). */
  val Min: ULID = new ULID(0L, 0L)

  /** The maximum possible ULID (all bits one). */
  val Max: ULID = new ULID(-1L, -1L)

  /** The minimum ULID for a given timestamp (random bits all zero). */
  def minFor(time: Long): ULID = new ULID(time << 16, 0L)

  /** The maximum ULID for a given timestamp (random bits all one). */
  def maxFor(time: Long): ULID = new ULID((time << 16) | 0xffffL, -1L)

  // ---- Constructors ----

  /**
   * Creates a ULID from raw MSB and LSB longs.
   */
  def apply(msb: Long, lsb: Long): ULID = new ULID(msb, lsb)

  /**
   * Creates a ULID from a 48-bit timestamp and 10 random bytes.
   *
   * @throws IllegalArgumentException if time overflows 48 bits or random is not 10 bytes
   */
  def apply(time: Long, random: Array[Byte]): ULID = {
    require((time & 0xffff000000000000L) == 0, s"Time overflows 48 bits: $time")
    require(random != null && random.length == RandomBytes, "Random component must be exactly 10 bytes")

    var msb = 0L
    var lsb = 0L

    msb |= time << 16
    msb |= (random(0) & 0xffL) << 8
    msb |= (random(1) & 0xffL)

    lsb |= (random(2) & 0xffL) << 56
    lsb |= (random(3) & 0xffL) << 48
    lsb |= (random(4) & 0xffL) << 40
    lsb |= (random(5) & 0xffL) << 32
    lsb |= (random(6) & 0xffL) << 24
    lsb |= (random(7) & 0xffL) << 16
    lsb |= (random(8) & 0xffL) << 8
    lsb |= (random(9) & 0xffL)

    new ULID(msb, lsb)
  }

  /**
   * Generates a fast, non-cryptographic ULID using [[ThreadLocalRandom]].
   * Suitable for logging and non-security-sensitive use cases.
   */
  def fast(): ULID = {
    val rng  = ThreadLocalRandom.current()
    val time = System.currentTimeMillis()
    new ULID((time << 16) | (rng.nextLong() & 0xffffL), rng.nextLong())
  }

  /**
   * Creates a ULID from a [[java.util.UUID]].
   */
  def fromUUID(uuid: UUID): ULID =
    new ULID(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)

  /**
   * Creates a ULID from a 16-byte array.
   *
   * @throws IllegalArgumentException if bytes is null or not 16 bytes
   */
  def fromBytes(bytes: Array[Byte]): ULID = {
    require(bytes != null && bytes.length == Bytes, "Must be exactly 16 bytes")
    var msb = 0L
    var lsb = 0L
    for (i <- 0 until 8)  msb = (msb << 8) | (bytes(i) & 0xffL)
    for (i <- 8 until 16) lsb = (lsb << 8) | (bytes(i) & 0xffL)
    new ULID(msb, lsb)
  }

  /**
   * Parses a ULID from a 26-character Crockford Base32 string.
   *
   * @throws IllegalArgumentException if the string is not a valid ULID
   */
  def fromString(s: String): ULID = {
    val chars = validated(s)
    var time  = 0L
    var rand0 = 0L
    var rand1 = 0L

    for (i <- 0  until 10) time  = (time  << 5) | (CharValues(chars(i).toInt) & 0xffL)
    for (i <- 10 until 18) rand0 = (rand0 << 5) | (CharValues(chars(i).toInt) & 0xffL)
    for (i <- 18 until 26) rand1 = (rand1 << 5) | (CharValues(chars(i).toInt) & 0xffL)

    val msb = (time << 16) | (rand0 >>> 24)
    val lsb = (rand0 << 40) | (rand1 & 0xffffffffffL)
    new ULID(msb, lsb)
  }

  /**
   * Returns `true` if the given string is a valid ULID.
   */
  def isValid(s: String): Boolean =
    s != null && s.length == StringChars && {
      val chars = s.toCharArray
      // First char must be 0–7 (top 2 bits of time must be zero per spec)
      (CharValues(chars(0).toInt) & 0x18) == 0 &&
      chars.forall { c =>
        try CharValues(c.toInt) != -1
        catch { case _: ArrayIndexOutOfBoundsException => false }
      }
    }

  private def validated(s: String): Array[Char] = {
    if (!isValid(s)) throw new IllegalArgumentException(s"Invalid ULID string: $s")
    s.toCharArray
  }
}
