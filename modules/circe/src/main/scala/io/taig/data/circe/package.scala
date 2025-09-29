package io.taig.data.circe

import cats.syntax.all.*
import io.circe.Json
import io.circe.JsonObject
import io.taig.data.Data

import java.math.BigDecimal as JBigDecimal
import java.math.BigInteger as JBigInteger

extension (self: Json)
  def toData: Data = self.fold(
    jsonNull = Data.Null,
    jsonBoolean = identity,
    jsonNumber = number =>
      number.toInt
        .orElse(number.toLong)
        .orElse(number.toFloat.some.filter(value => value != Float.NegativeInfinity && value != Float.PositiveInfinity))
        .orElse(
          number.toDouble.some.filter(value => value != Double.NegativeInfinity && value != Double.PositiveInfinity)
        )
        .orElse(number.toBigInt.map(_.bigInteger))
        .orElse(number.toBigDecimal.map(_.bigDecimal))
        .getOrElse(number.toDouble),
    jsonString = identity,
    jsonArray = _.toDataArray,
    jsonObject = _.toDataObject
  )

extension (self: JsonObject) def toDataObject: Data.Object[Data] = Data.Object(self.toList.map(_.map(_.toData)))

extension (self: Vector[Json]) def toDataArray: Data.Array[Data] = Data.Array(self.map(_.toData))

extension (self: Data)
  def toJson: Json = self match
    case data: Long              => Json.fromLong(data)
    case data: Int               => Json.fromInt(data)
    case data: Float             => Json.fromFloatOrString(data)
    case data: Double            => Json.fromDoubleOrString(data)
    case data: JBigDecimal       => Json.fromBigDecimal(BigDecimal(data))
    case data: JBigInteger       => Json.fromBigInt(BigInt(data))
    case data: Boolean           => Json.fromBoolean(data)
    case data: String            => Json.fromString(data)
    case data: Data.Object[Data] => Json.fromJsonObject(data.toJsonObject)
    case data: Data.Array[Data]  => Json.fromValues(data.toJsonArray)
    case Data.Null               => Json.Null

extension (self: Data.Object[?])
  def toJsonObject: JsonObject =
    JsonObject.fromIterable(self.values.map { case (key, value) => key -> value.toJson })

extension (self: Data.Array[Data]) def toJsonArray: Vector[Json] = self.values.map(_.toJson)
