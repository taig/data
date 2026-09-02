package io.taig.data

import cats.data.Chain
import cats.data.NonEmptyList
import cats.data.NonEmptyMap
import io.taig.data.syntax.*
import zio.test.*

import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Currency
import java.util.UUID
import scala.collection.immutable.SortedMap

object EncoderSpec extends ZIOSpecDefault:
  private val uuid: UUID = UUID.fromString("6d1b1c2c-3d3d-4e4e-8f8f-0a0a0a0a0a0a")

  def spec = suite("Encoder")(
    suite("numbers")(
      test("Int, Long, Float and Double encode as themselves") {
        assertTrue(1.asData == 1, 1L.asData == 1L, 1.5f.asData == 1.5f, 1.5d.asData == 1.5d)
      },
      test("Byte and Short widen to Int, because Data.Number has no member for them") {
        assertTrue((1: Byte).asData == 1, (1: Short).asData == 1)
      },
      test("Scala BigDecimal and BigInt encode as their Java counterparts") {
        assertTrue(
          BigDecimal("1.5").asData == java.math.BigDecimal("1.5"),
          BigInt("2").asData == java.math.BigInteger("2")
        )
      },
      test("boxed Java numbers unbox") {
        assertTrue(
          java.lang.Integer.valueOf(1).asData == 1,
          java.lang.Long.valueOf(1L).asData == 1L,
          java.lang.Byte.valueOf(1: Byte).asData == 1,
          java.lang.Double.valueOf(1.5d).asData == 1.5d
        )
      }
    ),
    suite("primitives")(
      test("Boolean and String encode as themselves") {
        assertTrue(true.asData == true, "a".asData == "a")
      },
      test("Char, UUID and URI encode as strings") {
        assertTrue(
          'a'.asData == "a",
          uuid.asData == "6d1b1c2c-3d3d-4e4e-8f8f-0a0a0a0a0a0a",
          URI.create("https://taig.io/").asData == "https://taig.io/"
        )
      },
      test("boxed Java Boolean and Character unbox") {
        assertTrue(java.lang.Boolean.valueOf(true).asData == true, java.lang.Character.valueOf('a').asData == "a")
      }
    ),
    suite("java.time")(
      test("instant-like values encode with their ISO representation") {
        assertTrue(
          Instant.parse("2026-08-17T12:00:00Z").asData == "2026-08-17T12:00:00Z",
          LocalDate.parse("2026-08-17").asData == "2026-08-17",
          LocalTime.parse("12:00:00").asData == "12:00:00",
          LocalDateTime.parse("2026-08-17T12:00:00").asData == "2026-08-17T12:00:00",
          OffsetTime.parse("12:00:00+02:00").asData == "12:00:00+02:00",
          OffsetDateTime.parse("2026-08-17T12:00:00+02:00").asData == "2026-08-17T12:00:00+02:00",
          ZonedDateTime.parse("2026-08-17T12:00:00+02:00[Europe/Berlin]").asData ==
            "2026-08-17T12:00:00+02:00[Europe/Berlin]"
        )
      },
      test("time-of-day values spell out the seconds, unlike toString") {
        assertTrue(
          LocalTime.of(9, 0).asData == "09:00:00",
          LocalDateTime.of(2026, 8, 17, 9, 0).asData == "2026-08-17T09:00:00",
          OffsetTime.of(9, 0, 0, 0, ZoneOffset.ofHours(2)).asData == "09:00:00+02:00",
          OffsetDateTime.of(2026, 8, 17, 9, 0, 0, 0, ZoneOffset.ofHours(2)).asData == "2026-08-17T09:00:00+02:00",
          ZonedDateTime.of(2026, 8, 17, 9, 0, 0, 0, ZoneId.of("Europe/Berlin")).asData ==
            "2026-08-17T09:00:00+02:00[Europe/Berlin]"
        )
      },
      test("amounts, zones and partial dates encode with their own formats") {
        assertTrue(
          Duration.ofSeconds(90).asData == "PT1M30S",
          Period.ofDays(2).asData == "P2D",
          ZoneId.of("Europe/Berlin").asData == "Europe/Berlin",
          ZoneOffset.ofHours(2).asData == "+02:00",
          MonthDay.of(8, 17).asData == "--08-17",
          Year.of(2026).asData == "2026",
          YearMonth.of(2026, 8).asData == "2026-08"
        )
      },
      // Scala.js ships a java.util.Currency whose registry is empty, so getInstance has nothing to look up there.
      test("Currency encodes as its code") {
        assertTrue(Currency.getInstance("EUR").asData == "EUR")
      } @@ TestAspect.jvmOnly
    ),
    suite("collections")(
      test("Foldable and Iterable collections encode as arrays") {
        assertTrue(
          List(1, 2).asData == arr(1, 2),
          Vector(1, 2).asData == arr(1, 2),
          Set(1).asData == arr(1),
          Chain(1, 2).asData == arr(1, 2),
          NonEmptyList.of(1, 2).asData == arr(1, 2),
          scala.Array(1, 2).asData == arr(1, 2),
          IArray(1, 2).asData == arr(1, 2)
        )
      },
      test("nested collections encode as nested arrays") {
        assertTrue(List(List(1), List(2)).asData == arr(arr(1), arr(2)))
      },
      test("tuples encode as arrays") {
        assertTrue(
          (1, "a").asData == arr(1, "a"),
          (1, "a", true).asData == arr(1, "a", true),
          EmptyTuple.asData == Data.Array.Empty
        )
      }
    ),
    suite("objects")(
      test("Unit encodes as an empty object") {
        assertTrue(().asData == Data.Object.Empty)
      },
      test("Map encodes as an object, with keys encoded by Encoder.Key") {
        assertTrue(
          Map("a" -> 1).asData == obj("a" := 1),
          Map(1 -> "a").asData == obj("1" := "a"),
          Map(uuid -> 1).asData == obj("6d1b1c2c-3d3d-4e4e-8f8f-0a0a0a0a0a0a" := 1),
          SortedMap("a" -> 1, "b" -> 2).asData == obj("a" := 1, "b" := 2)
        )
      },
      test("NonEmptyMap encodes as an object") {
        assertTrue(NonEmptyMap.of("a" -> 1, "b" -> 2).asData == obj("a" := 1, "b" := 2))
      }
    ),
    suite("Option")(
      test("Some encodes its value and None encodes as null") {
        assertTrue(Option(1).asData == 1, Option.empty[Int].asData == Data.Null, None.asData == Data.Null)
      },
      test("Some as its own type encodes its value") {
        assertTrue(Some(1).asData == 1)
      },
      test("a None inside a collection keeps its position") {
        assertTrue(List(Some(1), None).asData == arr(1, Data.Null))
      }
    ),
    suite("Data itself")(
      test("Data encodes as itself") {
        val data: Data = obj("a" := 1)
        val value: Data.Value = "a"
        val number: Data.Number = 1
        assertTrue(data.asData == data, value.asData == value, number.asDataNumber == number)
      },
      test("Data can be used as a field value") {
        val data: Data = arr(1, 2)
        assertTrue(obj("a" := data) == Data.Object(List("a" -> Data.Array(List(1, 2)))))
      }
    ),
    suite("combinators")(
      test("contramap keeps the refinement of the encoder it started from") {
        val encoder: Encoder.Number[String] = Encoder.Number[Int].contramap(_.length)
        assertTrue(encoder.encode("abc") == 3)
      },
      test("mapData rewrites the encoded value") {
        val encoder = Encoder[Int].mapData(_ => "replaced")
        assertTrue(encoder.encode(1) == "replaced")
      },
      test("the refined map variants keep their refinement") {
        val encoder: Encoder.Object[Map[String, Int]] =
          Encoder.Object[Map[String, Int]].mapDataObject(data => Data.Object(("count" -> data.values.size) :: Nil))
        assertTrue(encoder.encode(Map("a" -> 1, "b" -> 2)) == obj("count" := 2))
      },
      test("instance builds an encoder at each refinement") {
        val encoder = Encoder.Object.instance[Int](value => Data.Object(List("value" -> value)))
        assertTrue(encoder.encode(1) == obj("value" := 1))
      },
      test("recursive encodes a self-referential structure") {
        final case class Tree(value: Int, children: List[Tree])

        val encoder: Encoder[Tree] = Encoder.recursive { self =>
          given Encoder[Tree] = self
          Encoder.instance(tree => obj("value" := tree.value, "children" := tree.children))
        }

        assertTrue(
          encoder.encode(Tree(1, List(Tree(2, Nil)))) ==
            obj("value" := 1, "children" := arr(obj("value" := 2, "children" := Data.Array.Empty)))
        )
      }
    ),
    suite("Encoder.Key")(
      test("keys encode as strings") {
        assertTrue(
          Encoder.Key[String].encode("a") == "a",
          Encoder.Key[Symbol].encode(Symbol("a")) == "a",
          Encoder.Key[Int].encode(1) == "1",
          Encoder.Key[Long].encode(1L) == "1",
          Encoder.Key[Double].encode(1.5d) == "1.5",
          Encoder.Key[UUID].encode(uuid) == "6d1b1c2c-3d3d-4e4e-8f8f-0a0a0a0a0a0a"
        )
      }
    )
  )
