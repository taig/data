# Changelog

## 0.0.4

_2026-09-02_

- **Expand Encoder type coverage to match circe and add derivation**  
  Instances now cover java.time, UUID, URI, boxed Java numbers,
  Map/SortedMap/NonEmptyMap, tuples, and Option, plus Mirror-based derivation
  for case classes, sealed hierarchies and enums — bringing coverage from 16
  givens to 74, matching circe-core 0.14.16 type-for-type. Fixes two latent
  bugs surfaced along the way: `Option(1).asData` previously failed to
  compile because cats' `Foldable[Option]` was ambiguous with the dedicated
  `Option` encoder (fixed via a resolution priority ladder: identity types,
  then `Iterable`, then `Foldable`), and derived sum types would have
  recursed forever without deriving their cases structurally.
- **Pin a whole-minute regression case for time-of-day encoders**  
  `LocalTime.of(9, 0)` and its LocalDateTime/OffsetTime/OffsetDateTime/
  ZonedDateTime counterparts now get pinned assertions, so a future switch
  to `toString` would fail loudly instead of matching by coincidence on
  times that already carry seconds.
- **Add zio-test suite and document the Json.toData Infinity fallback**  
  Wires in zio-test (previously untested) and documents that `Json.toData`
  falls back to `Double.PositiveInfinity` for numbers whose exponent
  overflows `Int` — matching circe's own "nearest Double" contract rather
  than being an oversight.
- Upgrade to sbt 2
- Upgrade circe-core to 0.14.16
- Upgrade Scala to 3.3.8
- Add .jvmopts to raise the sbt heap limit for Scala.js linking
- Upgrade zio-test to 2.1.26
- Upgrade sbt to 2.0.8
- Upgrade sbt-ci-release to 1.12.1
- Replace deprecated sbt `url` with `uri` in build.sbt
- Upgrade actions/setup-java to v5
- Upgrade actions/checkout to v7

## 0.0.3

_2025-10-23_

- Add encoder instances for collection types
- Add project description
- Add basic Encoder
- Upgrade to scala 3.3.7
- Upgrade to sbt-houserules 0.11.7
- Upgrade to sbt 1.11.7

## 0.0.2

_2025-09-29_

- Fix package

## 0.0.1

_2025-09-29_

- Initial release
