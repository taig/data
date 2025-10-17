package io.taig.data

import io.taig.data.Data

object syntax:
  def arr[A <: Data](values: A*): Data.Array[A] = Data.Array(values.toVector)

  def obj[A <: Data](values: (String, A)*): Data.Object[A] = Data.Object(values.toList)

  extension [A](self: A)(using encoder: Encoder[A]) def asData: Data = encoder.encode(self)

  extension [A](self: A)(using encoder: Encoder.Value[A]) def asDataValue: Data.Value = encoder.encode(self)

  extension [A](self: A)(using encoder: Encoder.Primitive[A]) def asDataPrimitive: Data.Primitive = encoder.encode(self)

  extension [A](self: A)(using encoder: Encoder.Number[A]) def asDataNumber: Data.Number = encoder.encode(self)

  extension [A](self: A)(using encoder: Encoder.Array[A]) def asDataArray: Data.Array[?] = encoder.encode(self)

  extension [A](self: A)(using encoder: Encoder.Object[A]) def asDataObject: Data.Object[?] = encoder.encode(self)

  extension (self: String) def :=[A](data: A)(using encoder: Encoder[A]): (String, Data) = (self, data.asData)
