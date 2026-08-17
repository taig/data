# data

> Typed nested data: no more `Map[String, Any]`

## Encoding

`Encoder[A]` turns a value into `Data`. Its refinements say more about what comes out — `Encoder.Value`,
`Encoder.Primitive`, `Encoder.Number`, `Encoder.Array` and `Encoder.Object` — and the syntax package offers one
extension per level:

```scala
import io.taig.data.*
import io.taig.data.syntax.*

obj("name" := "Alice", "tags" := List("a", "b"), "since" := java.time.Instant.now)
```

Instances cover the primitives and their boxed Java counterparts, `BigDecimal`/`BigInt`, `Char`, `UUID`, `URI`,
`Unit`, the `java.time` types, `java.util.Currency`, `Option`, tuples of any arity, `Map` (keys via `Encoder.Key`),
`NonEmptyMap`, arrays, and every collection that is `Iterable` or has a cats `Foldable` instance — which includes
`Chain`, `NonEmptyList`, `NonEmptyVector`, `NonEmptyChain` and `NonEmptySet`. `Data` itself encodes as itself.

`Either` and `Validated` are deliberately absent: any wire format for them has to invent key names, so write the
encoder you want with `Encoder.Object.instance`.

## Derivation

Both derived shapes are fixed — there is no configuration — and you pick one by naming the type class:

```scala
final case class User(name: String, address: Address) derives Encoder.Object
// {"name": "Alice", "address": {…}}

enum Shape derives Encoder.Object:
  case Circle(radius: Double)
// {"Circle": {"radius": 1.5}}

enum Color derives Encoder.Primitive:
  case Red, Green
// "Red"
```

A product's fields use whichever encoder is in scope for their type, so a nested type with its own instance keeps it.
A sum's cases are always derived structurally; to change how a case looks, write the encoder for the whole sum by
hand. Recursive types derive as they are; for a hand-written recursive encoder, use `Encoder.recursive`.

## Scala.js

`java.time` is not part of the Scala.js javalib, so the `java.time` encoders link only if you add
[scala-java-time](https://github.com/cquiroz/scala-java-time) yourself. Everything else works out of the box.
`java.util.Currency` exists on Scala.js but has an empty registry, so `Currency.getInstance` finds nothing there.
