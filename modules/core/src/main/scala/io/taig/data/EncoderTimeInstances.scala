package io.taig.data

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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.Currency

/** Encoders for `java.time` (and `java.util.Currency`), producing the same strings as circe.
  *
  * Every instance is a lazy `given`, which keeps these classes out of a Scala.js linker's reachability graph unless
  * they are actually used — `java.time` is not part of the Scala.js javalib, so JS consumers that do use them need to
  * add `scala-java-time` themselves. This is the same trade-off circe makes.
  */
private[data] trait EncoderTimeInstances:
  private lazy val YearFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).toFormatter

  private lazy val YearMonthFormatter: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
    .appendLiteral('-')
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .toFormatter

  given Encoder.Primitive[Duration] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[Instant] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[Period] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[LocalDate] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[MonthDay] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[ZoneOffset] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[ZoneId] = Encoder.Primitive[String].contramap(_.getId)

  given Encoder.Primitive[LocalTime] = Encoder.temporal(DateTimeFormatter.ISO_LOCAL_TIME)
  given Encoder.Primitive[LocalDateTime] = Encoder.temporal(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  given Encoder.Primitive[OffsetTime] = Encoder.temporal(DateTimeFormatter.ISO_OFFSET_TIME)
  given Encoder.Primitive[OffsetDateTime] = Encoder.temporal(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  given Encoder.Primitive[ZonedDateTime] = Encoder.temporal(DateTimeFormatter.ISO_ZONED_DATE_TIME)
  given Encoder.Primitive[Year] = Encoder.temporal(YearFormatter)
  given Encoder.Primitive[YearMonth] = Encoder.temporal(YearMonthFormatter)

  given Encoder.Primitive[Currency] = Encoder.Primitive[String].contramap(_.getCurrencyCode)
