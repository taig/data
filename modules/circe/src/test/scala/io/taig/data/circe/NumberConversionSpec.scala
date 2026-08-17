package io.taig.data.circe

import io.circe.Json
import io.circe.JsonNumber
import zio.test.*

object NumberConversionSpec extends ZIOSpecDefault:
  def spec = suite("Json.toData number conversion")(
    test("a number too large to represent exactly falls back to the nearest Double, which may be Infinity") {
      // Exponent overflows Int, defeating Int/Long/finite Float/finite Double/BigInt (2^18-digit
      // limit)/BigDecimal (Int-scale limit). Infinity is then the correct "nearest Double" answer.
      val huge = JsonNumber.fromString("1e9999999999").get
      val data = Json.fromJsonNumber(huge).toData
      assertTrue(data == Double.PositiveInfinity)
    }
  )
