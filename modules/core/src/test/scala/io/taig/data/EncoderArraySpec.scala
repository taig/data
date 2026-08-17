package io.taig.data

import zio.test.*

object EncoderArraySpec extends ZIOSpecDefault:
  def spec = suite("Encoder.Array resolution")(
    test("SANITY CHECK: deliberately broken snippet must report an error") {
      val errors = scala.compiletime.testing.typeCheckErrors("""
        val x: Int = "definitely not an int"
      """)
      assertTrue(errors.nonEmpty)
    },
    test("List[Int] resolves without ambiguity") {
      val errors = scala.compiletime.testing.typeCheckErrors("""
        import io.taig.data.syntax.*
        List(1, 2, 3).asDataArray
      """)
      assertTrue(errors.isEmpty)
    },
    test("Vector[Int] resolves without ambiguity") {
      val errors = scala.compiletime.testing.typeCheckErrors("""
        import io.taig.data.syntax.*
        Vector(1, 2, 3).asDataArray
      """)
      assertTrue(errors.isEmpty)
    },
    test("Set[Int] resolves without ambiguity") {
      val errors = scala.compiletime.testing.typeCheckErrors("""
        import io.taig.data.syntax.*
        Set(1, 2, 3).asDataArray
      """)
      assertTrue(errors.isEmpty)
    }
  )
