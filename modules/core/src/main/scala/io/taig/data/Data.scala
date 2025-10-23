package io.taig.data

import cats.Eq
import cats.Show
import cats.syntax.all.*

import java.math.BigDecimal as JBigDecimal
import java.math.BigInteger as JBigInteger

type Data = Data.Value | Data.Null

object Data:
  type Value = Data.Primitive | Data.Object[?] | Data.Array[?]

  object Value:
    given show: Show[Data.Value] =
      case value: Data.Primitive => Primitive.show.show(value)
      case value: Data.Object[?] => value.show
      case value: Data.Array[?]  => value.show

  type Primitive = Number | Boolean | String

  object Primitive:
    given show: Show[Data.Primitive] =
      case value: Data.Number => Number.show.show(value)
      case value: Boolean     => String.valueOf(value)
      case value: String      => s"\"$value\""

  type Number = JBigDecimal | JBigInteger | Long | Int | Float | Double

  object Number:
    given show: Show[Data.Number] =
      case value: JBigDecimal                                 => String.valueOf(value)
      case value: (JBigInteger | Float | Double | Long | Int) => String.valueOf(value)

  final case class Object[+A <: Data](values: List[(String, A)]) extends AnyVal

  object Object:
    val Empty: Data.Object[Nothing] = Object(List.empty)

    given show[A <: Data]: Show[Data.Object[A]] = obj =>
      "{" + obj.values.map { case (key, value) => show"\"$key\":${Data.show.show(value)}" }.mkString(",") + "}"

  final case class Array[+A <: Data](values: List[A]) extends AnyVal

  object Array:
    val Empty: Data.Array[Nothing] = Array(List.empty)

    given show[A <: Data]: Show[Data.Array[A]] = array => "[" + array.values.map(Data.show.show).mkString(",") + "]"

  type Null = Data.Null.type
  case object Null

  given [A <: Data]: Eq[A] = Eq.fromUniversalEquals

  given show: Show[Data] =
    case value: Data.Value => Value.show.show(value)
    case _: Data.Null      => "null"
