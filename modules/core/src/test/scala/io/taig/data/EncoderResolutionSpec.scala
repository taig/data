package io.taig.data

import zio.test.*

/** Guards implicit resolution itself: many encoders overlap (`List` is both `Foldable` and `Iterable`, `Map` is both an
  * object and an `Iterable` of pairs, `Int` is both itself and a member of the `Data.Number` union), so every shape
  * below has to resolve to exactly one instance rather than an ambiguity.
  */
object EncoderResolutionSpec extends ZIOSpecDefault:
  private inline def resolves(name: String, inline code: String) =
    test(s"$name resolves without ambiguity") {
      val errors = scala.compiletime.testing.typeCheckErrors("import io.taig.data.syntax.*\n" + code)
      assert(errors.map(_.message))(Assertion.isEmpty)
    }

  def spec = suite("Encoder resolution")(
    test("SANITY CHECK: deliberately broken snippet must report an error") {
      val errors = scala.compiletime.testing.typeCheckErrors("""
        val x: Int = "definitely not an int"
      """)
      assertTrue(errors.nonEmpty)
    },
    suite("arrays")(
      resolves("List[Int]", "List(1, 2, 3).asDataArray"),
      resolves("Vector[Int]", "Vector(1, 2, 3).asDataArray"),
      resolves("Set[Int]", "Set(1, 2, 3).asDataArray"),
      resolves("SortedSet[Int]", "scala.collection.immutable.SortedSet(1, 2, 3).asDataArray"),
      resolves("Queue[Int]", "scala.collection.immutable.Queue(1, 2, 3).asDataArray"),
      resolves("Chain[Int]", "cats.data.Chain(1, 2, 3).asDataArray"),
      resolves("NonEmptyList[Int]", "cats.data.NonEmptyList.of(1, 2, 3).asDataArray"),
      resolves("NonEmptyVector[Int]", "cats.data.NonEmptyVector.of(1, 2, 3).asDataArray"),
      resolves("NonEmptyChain[Int]", "cats.data.NonEmptyChain(1, 2, 3).asDataArray"),
      resolves("NonEmptySet[Int]", "cats.data.NonEmptySet.of(1, 2, 3).asDataArray"),
      resolves("Array[Int]", "Array(1, 2, 3).asDataArray"),
      resolves("IArray[Int]", "IArray(1, 2, 3).asDataArray"),
      resolves("List[Option[Int]]", "List(Some(1), None).asDataArray"),
      resolves("List[List[Int]]", "List(List(1), List(2)).asDataArray"),
      resolves("(Int, String)", "(1, \"a\").asDataArray"),
      resolves("EmptyTuple", "EmptyTuple.asDataArray")
    ),
    suite("objects")(
      resolves("Unit", "().asDataObject"),
      resolves("Map[String, Int]", "Map(\"a\" -> 1).asDataObject"),
      resolves("Map[java.util.UUID, List[Int]]", "Map(java.util.UUID.randomUUID -> List(1)).asDataObject"),
      resolves("SortedMap[String, Int]", "scala.collection.immutable.SortedMap(\"a\" -> 1).asDataObject"),
      resolves("NonEmptyMap[String, Int]", "cats.data.NonEmptyMap.of(\"a\" -> 1).asDataObject"),
      resolves("Map[String, Int] as a plain Encoder", "Map(\"a\" -> 1).asData")
    ),
    suite("Data itself")(
      resolves("Data", "val data: io.taig.data.Data = 1; data.asData"),
      resolves("Data.Value", "val data: io.taig.data.Data.Value = 1; data.asDataValue"),
      resolves("Data.Primitive", "val data: io.taig.data.Data.Primitive = 1; data.asDataPrimitive"),
      resolves("Data.Number", "val data: io.taig.data.Data.Number = 1; data.asDataNumber"),
      resolves("Data.Array[Data]", "val data = io.taig.data.Data.Array(List(1)); data.asDataArray"),
      resolves("Data.Object[Data]", "val data = io.taig.data.Data.Object(List(\"a\" -> 1)); data.asDataObject"),
      resolves("Data as a field value", "val data: io.taig.data.Data = 1; obj(\"a\" := data)"),
      resolves("Data.Value as a plain Encoder", "val data: io.taig.data.Data.Value = 1; data.asData")
    ),
    suite("scalars keep their own instance")(
      resolves("Int", "1.asDataNumber"),
      resolves("Byte", "(1: Byte).asDataNumber"),
      resolves("String", "\"a\".asDataPrimitive"),
      resolves("Char", "'a'.asDataPrimitive"),
      resolves("java.time.Instant", "java.time.Instant.now.asDataPrimitive")
    )
  )
