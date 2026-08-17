package io.taig.data.circe

import io.circe.Json
import io.circe.syntax.*
import io.taig.data.Data
import io.taig.data.Encoder
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
import java.util.UUID

/** Checks that this library encodes the types it shares with circe into the same JSON circe does, by running both
  * encoders over the same value and comparing the results through [[toJson]].
  */
object EncoderParitySpec extends ZIOSpecDefault:
  private def parity[A](value: A)(using encoder: Encoder[A], circe: io.circe.Encoder[A]) =
    val expected = value.asJson
    val actual = value.asData.toJson
    assertTrue(actual == expected)

  private val uuid: UUID = UUID.fromString("6d1b1c2c-3d3d-4e4e-8f8f-0a0a0a0a0a0a")

  def spec = suite("parity with circe")(
    test("numbers") {
      parity(1) &&
      parity(1L) &&
      parity(1.5d) &&
      parity(1.5f) &&
      parity(1: Byte) &&
      parity(1: Short) &&
      parity(BigDecimal("1.5")) &&
      parity(BigInt("2"))
    },
    test("primitives") {
      parity(true) && parity("a") && parity('a') && parity(uuid) && parity(URI.create("https://taig.io/"))
    },
    test("java.time") {
      parity(Instant.parse("2026-08-17T12:00:00Z")) &&
      parity(LocalDate.parse("2026-08-17")) &&
      parity(LocalTime.parse("12:00:00")) &&
      parity(LocalDateTime.parse("2026-08-17T12:00:00")) &&
      parity(OffsetTime.parse("12:00:00+02:00")) &&
      parity(OffsetDateTime.parse("2026-08-17T12:00:00+02:00")) &&
      parity(ZonedDateTime.parse("2026-08-17T12:00:00+02:00[Europe/Berlin]")) &&
      parity(Duration.ofSeconds(90)) &&
      parity(Period.ofDays(2)) &&
      parity(ZoneId.of("Europe/Berlin")) &&
      parity(ZoneOffset.ofHours(2)) &&
      parity(MonthDay.of(8, 17)) &&
      parity(Year.of(2026)) &&
      parity(YearMonth.of(2026, 8))
    },
    test("collections") {
      parity(List(1, 2)) &&
      parity(Vector(1, 2)) &&
      parity(Set(1)) &&
      parity(List(List(1), List(2))) &&
      parity((1, "a")) &&
      parity(Map("a" -> 1)) &&
      parity(Map(1 -> "a"))
    },
    test("Option") {
      parity(Option(1)) && parity(Option.empty[Int]) && parity(List(Some(1), None))
    },
    test("Unit encodes as an empty object, as it does in circe") {
      assertTrue(().asData.toJson == Json.obj(), ().asJson == Json.obj())
    },
    test("a derived product matches a hand-written circe encoder") {
      final case class User(name: String, age: Int) derives Encoder.Object

      val user = User("Alice", 30)
      val expected = Json.obj("name" -> Json.fromString("Alice"), "age" -> Json.fromInt(30))
      assertTrue(user.asDataObject.toJsonObject == expected.asObject.get)
    },
    test("Data survives a round trip through circe's AST") {
      val data: Data = obj("a" := 1, "b" := arr(1, "x", Data.Null), "c" := obj("d" := true))
      assertTrue(data.toJson.toData == data)
    }
  )
