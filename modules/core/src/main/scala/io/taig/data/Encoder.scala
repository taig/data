package io.taig.data

import cats.Contravariant
import cats.Defer
import cats.Foldable
import cats.data.NonEmptyMap
import cats.syntax.all.*

import java.lang.Boolean as JBoolean
import java.lang.Byte as JByte
import java.lang.Character as JCharacter
import java.lang.Double as JDouble
import java.lang.Float as JFloat
import java.lang.Integer as JInteger
import java.lang.Long as JLong
import java.lang.Short as JShort
import java.math.BigDecimal as JBigDecimal
import java.math.BigInteger as JBigInteger
import java.net.URI
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.UUID
import scala.Array as SArray

trait Encoder[-A]:
  self =>

  def encode(a: A): Data

  def contramap[T](f: T => A): Encoder[T] = new Encoder[T]:
    def encode(t: T): Data = self.encode(f(t))

  def mapData(f: Data => Data): Encoder[A] = new Encoder[A]:
    def encode(a: A): Data = f(self.encode(a))

object Encoder extends EncoderInstances with EncoderTimeInstances:
  inline def apply[A](using encoder: Encoder[A]): Encoder[A] = encoder

  def instance[A](f: A => Data): Encoder[A] = f(_)

  /** Build an encoder in terms of itself, deferring the recursive reference until it is needed. */
  def recursive[A](f: Encoder[A] => Encoder[A]): Encoder[A] = Defer[Encoder].fix(f)

  /** Encode any `java.time` value as a string with the given formatter. */
  def temporal[A <: TemporalAccessor](formatter: DateTimeFormatter): Encoder.Primitive[A] =
    Encoder.Primitive[String].contramap(formatter.format)

  trait Value[-A] extends Encoder[A]:
    self =>

    override def encode(a: A): Data.Value

    override def contramap[T](f: T => A): Encoder.Value[T] = new Encoder.Value[T]:
      def encode(t: T): Data.Value = self.encode(f(t))

    def mapDataValue(f: Data.Value => Data.Value): Encoder.Value[A] = new Encoder.Value[A]:
      def encode(a: A): Data.Value = f(self.encode(a))

  object Value:
    inline def apply[A](using encoder: Encoder.Value[A]): Encoder.Value[A] = encoder

    def instance[A](f: A => Data.Value): Encoder.Value[A] = f(_)

    given Contravariant[Encoder.Value] with
      override def contramap[A, B](fa: Encoder.Value[A])(f: B => A): Encoder.Value[B] = fa.contramap(f)

  trait Primitive[-A] extends Encoder.Value[A]:
    self =>

    override def encode(a: A): Data.Primitive

    override def contramap[T](f: T => A): Encoder.Primitive[T] = new Encoder.Primitive[T]:
      def encode(t: T): Data.Primitive = self.encode(f(t))

    def mapDataPrimitive(f: Data.Primitive => Data.Primitive): Encoder.Primitive[A] = new Encoder.Primitive[A]:
      def encode(a: A): Data.Primitive = f(self.encode(a))

  object Primitive extends EncoderPrimitiveDerivation:
    inline def apply[A](using encoder: Encoder.Primitive[A]): Encoder.Primitive[A] = encoder

    def instance[A](f: A => Data.Primitive): Encoder.Primitive[A] = f(_)

    given Contravariant[Encoder.Primitive] with
      override def contramap[A, B](fa: Encoder.Primitive[A])(f: B => A): Encoder.Primitive[B] = fa.contramap(f)

  trait Number[-A] extends Encoder.Primitive[A]:
    self =>

    override def encode(a: A): Data.Number

    override def contramap[T](f: T => A): Encoder.Number[T] = new Encoder.Number[T]:
      def encode(t: T): Data.Number = self.encode(f(t))

    def mapDataNumber(f: Data.Number => Data.Number): Encoder.Number[A] = new Encoder.Number[A]:
      def encode(a: A): Data.Number = f(self.encode(a))

  object Number:
    inline def apply[A](using encoder: Encoder.Number[A]): Encoder.Number[A] = encoder

    def instance[A](f: A => Data.Number): Encoder.Number[A] = f(_)

    given Contravariant[Encoder.Number] with
      override def contramap[A, B](fa: Encoder.Number[A])(f: B => A): Encoder.Number[B] = fa.contramap(f)

  trait Array[-A] extends Encoder.Value[A]:
    self =>

    override def encode(a: A): Data.Array[?]

    override def contramap[T](f: T => A): Encoder.Array[T] = new Encoder.Array[T]:
      def encode(t: T): Data.Array[?] = self.encode(f(t))

    def mapDataArray(f: Data.Array[?] => Data.Array[?]): Encoder.Array[A] = new Encoder.Array[A]:
      def encode(a: A): Data.Array[?] = f(self.encode(a))

  object Array:
    inline def apply[A](using encoder: Encoder.Array[A]): Encoder.Array[A] = encoder

    def instance[A](f: A => Data.Array[?]): Encoder.Array[A] = f(_)

    given Contravariant[Encoder.Array] with
      override def contramap[A, B](fa: Encoder.Array[A])(f: B => A): Encoder.Array[B] = fa.contramap(f)

  trait Object[-A] extends Encoder.Value[A]:
    self =>

    override def encode(a: A): Data.Object[?]

    override def contramap[T](f: T => A): Encoder.Object[T] = new Encoder.Object[T]:
      def encode(t: T): Data.Object[?] = self.encode(f(t))

    def mapDataObject(f: Data.Object[?] => Data.Object[?]): Encoder.Object[A] = new Encoder.Object[A]:
      def encode(a: A): Data.Object[?] = f(self.encode(a))

  object Object extends EncoderObjectDerivation:
    inline def apply[A](using encoder: Encoder.Object[A]): Encoder.Object[A] = encoder

    def instance[A](f: A => Data.Object[?]): Encoder.Object[A] = f(_)

    given Contravariant[Encoder.Object] with
      override def contramap[A, B](fa: Encoder.Object[A])(f: B => A): Encoder.Object[B] = fa.contramap(f)

  /** Encodes the keys of a [[Data.Object]], which are plain strings and therefore not expressible as an [[Encoder]]. */
  trait Key[-A]:
    self =>

    def encode(a: A): String

    def contramap[T](f: T => A): Encoder.Key[T] = new Encoder.Key[T]:
      def encode(t: T): String = self.encode(f(t))

  object Key:
    inline def apply[A](using key: Encoder.Key[A]): Encoder.Key[A] = key

    def instance[A](f: A => String): Encoder.Key[A] = f(_)

    given Encoder.Key[String] = identity(_)
    given Encoder.Key[Symbol] = Encoder.Key[String].contramap(_.name)
    given Encoder.Key[Byte] = Encoder.Key[String].contramap(_.toString)
    given Encoder.Key[Short] = Encoder.Key[String].contramap(_.toString)
    given Encoder.Key[Int] = Encoder.Key[String].contramap(_.toString)
    given Encoder.Key[Long] = Encoder.Key[String].contramap(_.toString)
    given Encoder.Key[Double] = Encoder.Key[String].contramap(_.toString)
    given Encoder.Key[UUID] = Encoder.Key[String].contramap(_.toString)
    given Encoder.Key[URI] = Encoder.Key[String].contramap(_.toString)

    given Contravariant[Encoder.Key] with
      override def contramap[A, B](fa: Encoder.Key[A])(f: B => A): Encoder.Key[B] = fa.contramap(f)

  /** Encodes every element of a tuple, one encoder per position, which is what makes tuples of any arity encodable. */
  trait Elements[T <: Tuple]:
    def encode(t: T): List[Data]

  object Elements:
    inline def apply[T <: Tuple](using elements: Encoder.Elements[T]): Encoder.Elements[T] = elements

    given Encoder.Elements[EmptyTuple] = _ => List.empty

    given [H, T <: Tuple](using head: Encoder[H], tail: Encoder.Elements[T]): Encoder.Elements[H *: T] =
      t => head.encode(t.head) :: tail.encode(t.tail)

  given Contravariant[Encoder] with
    override def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = fa.contramap(f)

  given Defer[Encoder] with
    override def defer[A](fa: => Encoder[A]): Encoder[A] = new Encoder[A]:
      private lazy val encoder: Encoder[A] = fa

      def encode(a: A): Data = encoder.encode(a)

/** Identity encoders for [[Data]] itself, mirroring circe's `encodeJson`.
  *
  * These live at the bottom of a priority ladder — one tier per refinement of the [[Encoder]] hierarchy — because
  * [[Encoder]] is contravariant and the [[Data]] types are unions: `Encoder[Data]` is a valid `Encoder[Data.Value]`,
  * `Encoder.Value[Data.Value]` is a valid `Encoder.Value[Data.Primitive]`, and so on. Without the tiers every such pair
  * would be an ambiguous implicit rather than a resolution in favour of the most refined instance. Concrete instances
  * (`Int`, `String`, collections, …) sit above the whole ladder so they always win over the identity encoders.
  *
  * The whole ladder has to stay in this file: a parent of `object Encoder` that mentions `Data.Array` or `Data.Object`
  * cannot be compiled on its own, because completing it needs `Encoder`'s class file, which sends the compiler around
  * `Data.Array <: Data <: Data.Value <: Data.Array` and fails to unpickle `Data` with a cyclic reference.
  */
private[data] trait EncoderDataInstances:
  final protected def encodeAll[A](values: List[A])(using encoder: Encoder[A]): Data.Array[Data] =
    Data.Array(values.map(encoder.encode))

  final protected def encodeEntries[K, V](entries: List[(K, V)])(using
      key: Encoder.Key[K],
      value: Encoder[V]
  ): Data.Object[Data] = Data.Object(entries.map { case (k, v) => key.encode(k) -> value.encode(v) })

  given Encoder[Data] = identity(_)

private[data] trait EncoderDataValueInstances extends EncoderDataInstances:
  given Encoder.Value[Data.Value] = identity(_)

private[data] trait EncoderDataRefinedInstances extends EncoderDataValueInstances:
  given Encoder.Primitive[Data.Primitive] = identity(_)

  // Both are spelled with `Data` as the element type rather than a wildcard or a type parameter, which run into the
  // same cyclic reference described above. One concrete instance covers every element type anyway, because
  // `Data.Array` is covariant and `Encoder` contravariant.
  given Encoder.Array[Data.Array[Data]] = identity(_)
  given Encoder.Object[Data.Object[Data]] = identity(_)

private[data] trait EncoderDataNumberInstances extends EncoderDataRefinedInstances:
  given Encoder.Number[Data.Number] = identity(_)

/** Encoders for anything `Iterable`, the broadest and therefore lowest-priority way to reach an array.
  *
  * Collections that are also `Foldable` (`List`, `Vector`, …) resolve through the tier above; `Iterable`-only ones
  * (`Set`, `SortedSet`, `Queue`, …) land here.
  */
private[data] trait EncoderIterableInstances extends EncoderDataNumberInstances:
  given [A <: Iterable[?], B](using evidence: A <:< Iterable[B], encoder: Encoder[B]): Encoder.Array[A] =
    values => encodeAll(evidence(values).toList)

/** Encoders for sequential containers.
  *
  * `Foldable` is deliberately below the instances in [[EncoderInstances]]: cats also provides `Foldable[Option]`,
  * `Traverse[Map[K, *]]` and `Traverse[NonEmptyMap[K, *]]`, so without this tier an `Option` would be as good an array
  * as it is a value, and a `Map` as good an array of pairs as it is an object.
  */
private[data] trait EncoderFoldableInstances extends EncoderIterableInstances:
  given [F[_]: Foldable, A: Encoder]: Encoder.Array[F[A]] = values => encodeAll(values.toList)
  given [A: Encoder]: Encoder.Array[SArray[A]] = values => encodeAll(values.toList)
  given [A: Encoder]: Encoder.Array[IArray[A]] = values => encodeAll(values.toList)

private[data] trait EncoderInstances extends EncoderFoldableInstances:
  // Above the `Foldable` tier as well: cats provides `Traverse[(A, *)]`, which would otherwise encode a pair as an
  // array holding only its second element.
  given [T <: Tuple](using elements: Encoder.Elements[T]): Encoder.Array[T] = t => Data.Array(elements.encode(t))

  given Encoder.Number[Int] = identity(_)
  given Encoder.Number[Long] = identity(_)
  given Encoder.Number[Float] = identity(_)
  given Encoder.Number[Double] = identity(_)
  given Encoder.Number[JBigDecimal] = identity(_)
  given Encoder.Number[JBigInteger] = identity(_)
  given Encoder.Number[BigDecimal] = Encoder.Number[JBigDecimal].contramap(_.bigDecimal)
  given Encoder.Number[BigInt] = Encoder.Number[JBigInteger].contramap(_.bigInteger)

  // Data.Number has no Byte or Short member, so both widen to Int rather than encoding as themselves.
  given Encoder.Number[Byte] = Encoder.Number[Int].contramap(_.toInt)
  given Encoder.Number[Short] = Encoder.Number[Int].contramap(_.toInt)

  given Encoder.Number[JByte] = Encoder.Number[Byte].contramap(_.byteValue)
  given Encoder.Number[JShort] = Encoder.Number[Short].contramap(_.shortValue)
  given Encoder.Number[JInteger] = Encoder.Number[Int].contramap(_.intValue)
  given Encoder.Number[JLong] = Encoder.Number[Long].contramap(_.longValue)
  given Encoder.Number[JFloat] = Encoder.Number[Float].contramap(_.floatValue)
  given Encoder.Number[JDouble] = Encoder.Number[Double].contramap(_.doubleValue)

  given Encoder.Primitive[Boolean] = identity(_)
  given Encoder.Primitive[String] = identity(_)
  given Encoder.Primitive[Char] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[JBoolean] = Encoder.Primitive[Boolean].contramap(_.booleanValue)
  given Encoder.Primitive[JCharacter] = Encoder.Primitive[Char].contramap(_.charValue)
  given Encoder.Primitive[UUID] = Encoder.Primitive[String].contramap(_.toString)
  given Encoder.Primitive[URI] = Encoder.Primitive[String].contramap(_.toString)

  given Encoder.Object[Unit] = _ => Data.Object.Empty

  given [K: Encoder.Key, V: Encoder, M[K, V] <: Map[K, V]]: Encoder.Object[M[K, V]] =
    values => encodeEntries(values.toList)

  given [K: Encoder.Key, V: Encoder]: Encoder.Object[NonEmptyMap[K, V]] = values => encodeEntries(values.toNel.toList)

  given [A](using encoder: Encoder[A]): Encoder[Option[A]] =
    case Some(value) => encoder.encode(value)
    case None        => Data.Null

  given [A](using encoder: Encoder[A]): Encoder[Some[A]] = some => encoder.encode(some.value)

  given Encoder[None.type] = _ => Data.Null
