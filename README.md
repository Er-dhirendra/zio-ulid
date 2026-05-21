# zio-ulid

[![CI](https://github.com/Er-dhirendra/zio-ulid/actions/workflows/ci.yml/badge.svg)](https://github.com/Er-dhirendra/zio-ulid/actions/workflows/ci.yml)
[![Release](https://github.com/Er-dhirendra/zio-ulid/actions/workflows/release.yml/badge.svg)](https://github.com/Er-dhirendra/zio-ulid/actions/workflows/release.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.zio/zio-ulid_3.svg)](https://central.sonatype.com/artifact/dev.zio/zio-ulid_3)
[![Scala Steward](https://img.shields.io/badge/Scala_Steward-helping-blue.svg)](https://scala-steward.org)

> Type-safe, purely functional ULID generation for ZIO applications.

Documentation is also available in [README.adoc](README.adoc) (AsciiDoc).

## What is a ULID?

A **ULID** (Universally Unique Lexicographically Sortable Identifier) is a 128-bit identifier that is:

- **Sortable** — lexicographic order matches creation time order
- **URL-safe** — 26 Crockford Base32 characters, no special characters
- **UUID-compatible** — same 128-bit size; can be stored in UUID columns
- **Human-readable** — `01ARZ3NDEKTSV4RRFFQ69G5FAV`

See the [ULID specification](https://github.com/ulid/spec) for the full format.

```
 01ARZ3NDEKTSV4RRFFQ69G5FAV
 |------||----------------|
 time(10)   random(16)
```

## Installation

Add the dependency to `build.sbt`:

```scala
libraryDependencies += "dev.zio" %% "zio-ulid" % "0.1.0"
```

Maven coordinates:

- `dev.zio:zio-ulid_3:0.1.0` (Scala 3.3)
- `dev.zio:zio-ulid_2.13:0.1.0` (Scala 2.13)

Requires ZIO 2.x.

## Quick Start

```scala
import zio._
import zio.ulid._

val program: ZIO[ULIDGen, Nothing, Unit] =
  for {
    id <- ULIDGen.generate
    _  <- ZIO.debug(s"Generated: $id")
    // Generated: 01ARZ3NDEKTSV4RRFFQ69G5FAV
  } yield ()

program.provide(ULIDGen.live)
```

## Generation Strategies

| Layer | Random source | Use case |
| --- | --- | --- |
| `ULIDGen.live` | `SecureRandom` | Production — cryptographically secure |
| `ULIDGen.monotonic` | `SecureRandom` + increment | Database IDs and workloads requiring strict ordering |
| `ULIDGen.fast` | `ThreadLocalRandom` | Logging, tracing, non-security-sensitive workloads |
| `ULIDGen.deterministic(t)` | Counter from `minFor(t)` | Unit tests — reproducible, ordered output |

### Monotonic generation

When high throughput generates multiple IDs within the same millisecond, `ULIDGen.monotonic` increments the random component instead of re-randomizing it, guaranteeing strict lexicographic order:

```scala
val ids: ZIO[ULIDGen, Nothing, Chunk[ULID]] =
  ULIDGen.generateN(1000).provide(ULIDGen.monotonic)
// id(n) < id(n+1) for every n, even within the same millisecond
```

### Deterministic testing

Use the deterministic layer with [MUnit](https://scalameta.org/munit/) or any test runner:

```scala
import munit.FunSuite
import zio.ulid._
import zio.ulid.test.ZioTestSupport

class MySuite extends FunSuite {
  test("IDs are strictly ordered") {
    val (a, b) = ZioTestSupport.runWith(ULIDGen.deterministic(0L)) {
      for {
        x <- ULIDGen.generate
        y <- ULIDGen.generate
      } yield (x, y)
    }
    assert(a < b)
  }
}
```

## Working with ULIDs

```scala
import java.time.Instant
import zio.ulid._

val ulid = ULID.fast()

// String representation
ulid.toString    // "01ARZ3NDEKTSV4RRFFQ69G5FAV" (uppercase)
ulid.toLowerCase // "01arz3ndektsv4rrffq69g5fav"

// Parse from string
val parsed = ULID.fromString("01ARZ3NDEKTSV4RRFFQ69G5FAV")
ULID.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAV") // true

// Time component
ulid.time    // milliseconds since Unix epoch
ulid.instant // java.time.Instant

// Conversions
ulid.toUUID    // java.util.UUID
ulid.toBytes   // Array[Byte] (16 bytes)
ulid.toRfc4122 // RFC-4122 UUIDv4-compatible (not reversible)

// Range queries (records in a time window)
val from = ULID.minFor(Instant.parse("2024-01-01T00:00:00Z").toEpochMilli)
val to   = ULID.maxFor(Instant.parse("2024-12-31T23:59:59Z").toEpochMilli)
```

Construct a ULID without the ZIO service (non-cryptographic):

```scala
val ulid = ULID.fast() // ThreadLocalRandom
```

## Comparison to UUID

| Feature | UUID v4 | ULID |
| --- | --- | --- |
| Sortable | ❌ | ✅ |
| URL-safe | ❌ (hyphens) | ✅ |
| Human-readable | ❌ | ✅ |
| Monotonic option | ❌ | ✅ |
| Size | 128 bits | 128 bits |
| Charset | hex + hyphens | Crockford Base32 |

## Development

Prerequisites: JDK 17+, sbt 1.10+.

```bash
# Run all tests (default Scala 3.3.6)
sbt core/test

# Cross-build
sbt ++2.13.16 core/test

# Formatting
sbt fmt
sbt check
```

Tests use [MUnit](https://scalameta.org/munit/). ZIO-based generator tests run effects via `zio.ulid.test.ZioTestSupport`.

CI runs on every push and pull request to `main` (see [ci.yml](.github/workflows/ci.yml)).

## Publishing

Releases are automated with GitHub Actions and [sbt-ci-release](https://github.com/sbt/sbt-ci-release):

- **Snapshots** — each push to `main` publishes a unique `-SNAPSHOT` (via `sbt-dynver`)
- **Stable releases** — push a git tag such as `v0.1.0` to trigger [release.yml](.github/workflows/release.yml)

Setup instructions (Sonatype Central Portal, GPG keys, GitHub secrets): [docs/PUBLISHING.adoc](docs/PUBLISHING.adoc).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
