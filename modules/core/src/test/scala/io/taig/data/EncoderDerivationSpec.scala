package io.taig.data

import io.taig.data.syntax.*
import zio.test.*

object EncoderDerivationSpec extends ZIOSpecDefault:
  final case class Address(street: String, zip: Int) derives Encoder.Object

  final case class User(name: String, age: Option[Int], address: Address, tags: List[String]) derives Encoder.Object

  case object Unnamed derives Encoder.Object

  enum Color derives Encoder.Primitive:
    case Red, Green

  enum Shape derives Encoder.Object:
    case Circle(radius: Double)
    case Square(side: Double)

  enum Flag derives Encoder.Object:
    case On, Off

  final case class Tree(value: Int, children: List[Tree]) derives Encoder.Object

  def spec = suite("Encoder derivation")(
    suite("products")(
      test("a case class encodes as an object keyed by its field names, in declaration order") {
        assertTrue(Address("Main Street", 1).asDataObject == obj("street" := "Main Street", "zip" := 1))
      },
      test("nested products, options and collections derive through their own encoders") {
        assertTrue(
          User("Alice", Some(30), Address("Main Street", 1), List("a")).asDataObject == obj(
            "name" := "Alice",
            "age" := 30,
            "address" := obj("street" := "Main Street", "zip" := 1),
            "tags" := arr("a")
          )
        )
      },
      test("a None field encodes as null rather than being dropped") {
        assertTrue(
          User("Alice", None, Address("Main Street", 1), Nil).asDataObject == obj(
            "name" := "Alice",
            "age" := Data.Null,
            "address" := obj("street" := "Main Street", "zip" := 1),
            "tags" := Data.Array.Empty
          )
        )
      },
      test("a case object encodes as an empty object") {
        assertTrue(Unnamed.asDataObject == Data.Object.Empty)
      },
      test("a recursive product derives without looping while the instance is still being built") {
        assertTrue(
          Tree(1, List(Tree(2, Nil))).asDataObject == obj(
            "value" := 1,
            "children" := arr(obj("value" := 2, "children" := Data.Array.Empty))
          )
        )
      }
    ),
    suite("sums")(
      test("a case encodes into a single-field wrapper object named after it") {
        assertTrue(
          (Shape.Circle(1.5d): Shape).asDataObject == obj("Circle" := obj("radius" := 1.5d)),
          (Shape.Square(2.0d): Shape).asDataObject == obj("Square" := obj("side" := 2.0d))
        )
      },
      test("a parameterless case encodes as a wrapper around an empty object") {
        assertTrue((Flag.On: Flag).asDataObject == obj("On" := Data.Object.Empty))
      }
    ),
    suite("enums as strings")(
      test("Encoder.Primitive derives the case name") {
        assertTrue((Color.Red: Color).asDataPrimitive == "Red", (Color.Green: Color).asDataPrimitive == "Green")
      },
      test("a sum with parameterised cases is rejected, pointing at Encoder.Object") {
        val errors = scala.compiletime.testing.typeCheckErrors("""
          enum Shape derives Encoder.Primitive:
            case Circle(radius: Double)
        """)
        assertTrue(errors.exists(_.message.contains("Encoder.Object")))
      }
    )
  )
