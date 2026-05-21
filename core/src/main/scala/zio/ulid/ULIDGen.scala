package zio.ulid

import zio.{Chunk, Clock, UIO, ULayer, ZIO, ZLayer}

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference

/** ZIO service for generating [[ULID]]s.
  *
  * Two generation strategies are available:
  *   - '''Standard''': Fresh random bits for every ULID generated.
  *   - '''Monotonic''': Within the same millisecond, the random component is incremented by 1,
  *     guaranteeing strict lexicographic ordering even at high throughput.
  *
  * Usage:
  * {{{
  *   val program: ZIO[ULIDGen, Nothing, ULID] =
  *     ULIDGen.generate
  *
  *   program.provide(ULIDGen.live)
  * }}}
  */
trait ULIDGen {

  /** Generates a new [[ULID]]. */
  def generate: UIO[ULID]

  /** Generates `n` unique [[ULID]]s in order. */
  def generateN(n: Int): UIO[Chunk[ULID]] =
    ZIO.replicateZIO(n)(generate).map(Chunk.fromIterable)
}

object ULIDGen {

  // ---- Accessors ----

  /** Generates a new [[ULID]] using the [[ULIDGen]] service from the environment. */
  val generate: ZIO[ULIDGen, Nothing, ULID] =
    ZIO.serviceWithZIO[ULIDGen](_.generate)

  /** Generates `n` [[ULID]]s using the [[ULIDGen]] service from the environment. */
  def generateN(n: Int): ZIO[ULIDGen, Nothing, Chunk[ULID]] =
    ZIO.serviceWithZIO[ULIDGen](_.generateN(n))

  // ---- Layers ----

  /** Standard live layer: cryptographically secure, fresh random per ULID.
    */
  val live: ULayer[ULIDGen] =
    ZLayer.fromZIO(makeStandard)

  /** Monotonic live layer: guarantees ordering within the same millisecond. Recommended for
    * database IDs and any use case requiring strict ordering.
    */
  val monotonic: ULayer[ULIDGen] =
    ZLayer.fromZIO(makeMonotonic)

  /** Fast layer: uses [[java.util.concurrent.ThreadLocalRandom]] — not cryptographically secure.
    * Suitable for logging and tracing.
    */
  val fast: ULayer[ULIDGen] =
    ZLayer.succeed(fastGen)

  // ---- Constructors ----

  /** Creates a standard (non-monotonic) [[ULIDGen]] backed by [[SecureRandom]].
    */
  val makeStandard: UIO[ULIDGen] =
    ZIO.succeed(secureGen(monotonic = false))

  /** Creates a monotonic [[ULIDGen]].
    *
    * Within a single millisecond the random component is incremented by 1 instead of being
    * re-randomised, guaranteeing that every generated ULID is strictly greater than the previous
    * one.
    */
  val makeMonotonic: UIO[ULIDGen] =
    ZIO.succeed(secureGen(monotonic = true))

  // ---- Test support ----

  /** Creates a deterministic [[ULIDGen]] for testing that generates ULIDs from a fixed sequence
    * starting at the given time with zero random bits, incrementing on each call.
    */
  def deterministic(startTimeMs: Long = 0L): ULayer[ULIDGen] =
    ZLayer.succeed(deterministicGen(startTimeMs))

  // ---- Implementations ----

  private val fastGen: ULIDGen =
    new ULIDGen {
      override def generate: UIO[ULID] =
        ZIO.succeed(ULID.fast())
    }

  private def secureGen(monotonic: Boolean): ULIDGen = {
    val rng      = new SecureRandom()
    val stateRef = new AtomicReference[ULID](ULID.Min)

    new ULIDGen {
      override def generate: UIO[ULID] =
        Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS).flatMap { time =>
          ZIO.succeed {
            if (monotonic) nextMonotonic(time, rng, stateRef)
            else ULID(time, randomBytes(rng))
          }
        }
    }
  }

  private def deterministicGen(startTimeMs: Long): ULIDGen = {
    val stateRef = new AtomicReference[ULID](ULID.minFor(startTimeMs))
    new ULIDGen {
      override def generate: UIO[ULID] =
        ZIO.succeed(casUpdate(stateRef)(_.increment))
    }
  }

  /** CAS loop: read current, compute next, commit next; return the value that was current. */
  private def casUpdate(stateRef: AtomicReference[ULID])(nextOf: ULID => ULID): ULID = {
    var result: ULID = null
    while (result == null) {
      val current = stateRef.get()
      val next    = nextOf(current)
      if (stateRef.compareAndSet(current, next))
        result = current
    }
    result
  }

  private def nextMonotonic(
    time: Long,
    rng: SecureRandom,
    stateRef: AtomicReference[ULID],
  ): ULID = {
    var result: ULID = null
    while (result == null) {
      val last     = stateRef.get()
      val lastTime = last.time
      val next =
        if (time <= lastTime) last.increment
        else ULID(time, randomBytes(rng))
      if (stateRef.compareAndSet(last, next))
        result = next
    }
    result
  }

  private def randomBytes(rng: SecureRandom): Array[Byte] = {
    val buf = new Array[Byte](ULID.RandomBytes)
    rng.nextBytes(buf)
    buf
  }
}
