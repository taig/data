package io.taig.data

import cats.Contravariant

import java.math.BigDecimal as JBigDecimal
import java.math.BigInteger as JBigInteger

trait Encoder[-A]:
  self =>

  def encode(a: A): Data

  def contramap[T](f: T => A): Encoder[T] = new Encoder[T]:
    def encode(t: T): Data = self.encode(f(t))

object Encoder:
  inline def apply[A](using encoder: Encoder[A]): Encoder[A] = encoder

  trait Value[-A] extends Encoder[A]:
    self =>

    override def encode(a: A): Data.Value

    override def contramap[T](f: T => A): Encoder.Value[T] = new Encoder.Value[T]:
      def encode(t: T): Data.Value = self.encode(f(t))

  object Value:
    inline def apply[A](using encoder: Encoder.Value[A]): Encoder.Value[A] = encoder

    given Contravariant[Encoder.Value] with
      override def contramap[A, B](fa: Encoder.Value[A])(f: B => A): Encoder.Value[B] = fa.contramap(f)

  trait Primitive[-A] extends Encoder.Value[A]:
    self =>

    override def encode(a: A): Data.Primitive

    override def contramap[T](f: T => A): Encoder.Primitive[T] = new Encoder.Primitive[T]:
      def encode(t: T): Data.Primitive = self.encode(f(t))

  object Primitive:
    inline def apply[A](using encoder: Encoder.Primitive[A]): Encoder.Primitive[A] = encoder

    given Contravariant[Encoder.Primitive] with
      override def contramap[A, B](fa: Encoder.Primitive[A])(f: B => A): Encoder.Primitive[B] = fa.contramap(f)

  trait Number[-A] extends Encoder.Primitive[A]:
    self =>

    override def encode(a: A): Data.Number

    override def contramap[T](f: T => A): Encoder.Number[T] = new Encoder.Number[T]:
      def encode(t: T): Data.Number = self.encode(f(t))

  object Number:
    inline def apply[A](using encoder: Encoder.Number[A]): Encoder.Number[A] = encoder

    given Contravariant[Encoder.Number] with
      override def contramap[A, B](fa: Encoder.Number[A])(f: B => A): Encoder.Number[B] = fa.contramap(f)

  trait Array[-A] extends Encoder.Value[A]:
    self =>

    override def encode(a: A): Data.Array[?]

    override def contramap[T](f: T => A): Encoder.Array[T] = new Encoder.Array[T]:
      def encode(t: T): Data.Array[?] = self.encode(f(t))

  object Array:
    inline def apply[A](using encoder: Encoder.Array[A]): Encoder.Array[A] = encoder

    given Contravariant[Encoder.Array] with
      override def contramap[A, B](fa: Encoder.Array[A])(f: B => A): Encoder.Array[B] = fa.contramap(f)

  trait Object[-A] extends Encoder.Value[A]:
    self =>

    override def encode(a: A): Data.Object[?]

    override def contramap[T](f: T => A): Encoder.Object[T] = new Encoder.Object[T]:
      def encode(t: T): Data.Object[?] = self.encode(f(t))

  object Object:
    inline def apply[A](using encoder: Encoder.Object[A]): Encoder.Object[A] = encoder

    given Contravariant[Encoder.Object] with
      override def contramap[A, B](fa: Encoder.Object[A])(f: B => A): Encoder.Object[B] = fa.contramap(f)

  given Encoder.Number[Int] = identity(_)
  given Encoder.Number[Double] = identity(_)
  given Encoder.Number[Long] = identity(_)
  given Encoder.Number[Float] = identity(_)
  given Encoder.Number[JBigDecimal] = identity(_)
  given Encoder.Number[JBigInteger] = identity(_)
  given Encoder.Number[BigDecimal] = Encoder.Number[JBigDecimal].contramap(_.bigDecimal)
  given Encoder.Number[BigInt] = Encoder.Number[JBigInteger].contramap(_.bigInteger)
  given Encoder.Primitive[Boolean] = identity(_)
  given Encoder.Primitive[String] = identity(_)

  given [A](using encoder: Encoder[A]): Encoder.Array[Vector[A]] =
    values => Data.Array(values.map(encoder.encode))

  given [A: Encoder]: Encoder.Array[List[A]] =
    Encoder.Array[Vector[A]].contramap(_.toVector)

  given [A](using encoder: Encoder[A]): Encoder[Option[A]] =
    case Some(value) => Encoder[A].encode(value)
    case None        => Data.Null

  given Contravariant[Encoder] with
    override def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = fa.contramap(f)
