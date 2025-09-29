package io.taig.data

import io.taig.data.Data

object syntax:
  def arr[A <: Data](values: A*): Data.Array[A] = Data.Array(values.toVector)

  def obj[A <: Data](values: (String, A)*): Data.Object[A] =
    Data.Object(values.toList)

  extension (self: String) def :=[A <: Data](data: A): (String, A) = (self, data)
