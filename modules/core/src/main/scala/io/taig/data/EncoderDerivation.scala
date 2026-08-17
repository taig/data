package io.taig.data

import scala.compiletime.constValue
import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.compiletime.summonFrom
import scala.compiletime.summonInline
import scala.deriving.Mirror

private[data] trait EncoderObjectDerivation:
  /** Derive an [[Encoder.Object]] for a product or a sum.
    *
    * A product becomes an object keyed by its field names, in declaration order. A sum becomes a single-field wrapper
    * object keyed by the case name, `{"CaseName": …}`. Both shapes are fixed: there is no configuration, so anything
    * else is written by hand with [[Encoder.Object.instance]].
    *
    * A product's fields are encoded by whichever encoder is in scope for their type, falling back to deriving one. A
    * sum's cases are always derived structurally instead — see [[EncoderDerivation.caseEncoders]].
    */
  inline def derived[A](using mirror: Mirror.Of[A]): Encoder.Object[A] =
    inline mirror match
      case product: Mirror.ProductOf[A] =>
        EncoderDerivation.product(
          EncoderDerivation.labels[product.MirroredElemLabels],
          EncoderDerivation.fieldEncoders[product.MirroredElemTypes],
          summonInline[A <:< Product]
        )
      case sum: Mirror.SumOf[A] =>
        EncoderDerivation.sum(
          EncoderDerivation.labels[sum.MirroredElemLabels],
          EncoderDerivation.caseEncoders[sum.MirroredElemTypes],
          sum.ordinal
        )

private[data] trait EncoderPrimitiveDerivation:
  /** Derive an [[Encoder.Primitive]] for a sum whose cases are all singletons, encoding the case name as a string.
    *
    * This is the enum counterpart to [[Encoder.Object.derived]]: `enum Color derives Encoder.Primitive` encodes as
    * `"Red"`, whereas `derives Encoder.Object` encodes as `{"Red": {}}`.
    */
  inline def derived[A](using mirror: Mirror.SumOf[A]): Encoder.Primitive[A] =
    EncoderDerivation.requireSingletons[mirror.MirroredElemTypes]
    EncoderDerivation.enumeration(EncoderDerivation.labels[mirror.MirroredElemLabels], mirror.ordinal)

private[data] object EncoderDerivation:
  /** Encodes anything that carries no data, which is what a parameterless case of a sum amounts to.
    *
    * Typed on `Any` rather than cast into place: [[Encoder]] is contravariant, so this already is an
    * `Encoder.Object[A]` for every `A`.
    */
  private val empty: Encoder.Object[Any] = _ => Data.Object.Empty

  def product[A](
      labels: List[String],
      encoders: List[() => Encoder[Any]],
      evidence: A <:< Product
  ): Encoder.Object[A] = new Encoder.Object[A]:
    private lazy val resolved: List[Encoder[Any]] = encoders.map(_())

    def encode(a: A): Data.Object[?] =
      val values = evidence(a).productIterator.toList
      Data.Object(labels.zip(values.zip(resolved)).map { case (label, (value, encoder)) =>
        label -> encoder.encode(value)
      })

  def sum[A](labels: List[String], encoders: List[() => Encoder[Any]], ordinal: A => Int): Encoder.Object[A] =
    new Encoder.Object[A]:
      private lazy val resolved: List[Encoder[Any]] = encoders.map(_())

      def encode(a: A): Data.Object[?] =
        val index = ordinal(a)
        Data.Object(List(labels(index) -> resolved(index).encode(a)))

  def enumeration[A](labels: List[String], ordinal: A => Int): Encoder.Primitive[A] = a => labels(ordinal(a))

  inline def labels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple     => List.empty
      case _: (head *: tail) => constValue[head].toString :: labels[tail]

  /** Encoders for the fields of a product, taken from implicit scope and derived only as a fallback.
    *
    * The thunks matter for recursive types: `case class Tree(children: List[Tree])` resolves `Encoder[Tree]` to the
    * very given being initialised, so the reference has to stay behind a function until the instance exists.
    */
  inline def fieldEncoders[T <: Tuple]: List[() => Encoder[Any]] =
    inline erasedValue[T] match
      case _: EmptyTuple     => List.empty
      case _: (head *: tail) => (() => erase(fieldEncoder[head])) :: fieldEncoders[tail]

  private inline def fieldEncoder[A]: Encoder[A] =
    summonFrom {
      case encoder: Encoder[A] => encoder
      case _                   => Encoder.Object.derived[A](using summonInline[Mirror.Of[A]])
    }

  /** Encoders for the cases of a sum, always derived structurally.
    *
    * Taking these from implicit scope would be wrong here rather than merely surprising: [[Encoder]] is contravariant,
    * so an `Encoder[A]` for the whole sum also satisfies `Encoder[Case]` — and the nearest candidate is the sum's own
    * encoder, the one currently being derived, which would encode every case by recursing into itself forever.
    */
  inline def caseEncoders[T <: Tuple]: List[() => Encoder[Any]] =
    inline erasedValue[T] match
      case _: EmptyTuple     => List.empty
      case _: (head *: tail) => (() => erase(caseEncoder[head])) :: caseEncoders[tail]

  private inline def caseEncoder[A]: Encoder[A] =
    summonFrom {
      // A parameterless case has nothing to encode, and is not a Product, so it cannot take the derivation below.
      case _: ValueOf[A]        => empty
      case mirror: Mirror.Of[A] => Encoder.Object.derived[A](using mirror)
    }

  /** The one cast this derivation needs: a `Mirror` offers no typed field access for encoding, only the untyped
    * `Product.productIterator` and, for sums, an ordinal. Both index into these encoders by the position each one was
    * summoned for, so the widening cannot be observed.
    */
  private def erase[A](encoder: Encoder[A]): Encoder[Any] =
    encoder.asInstanceOf[Encoder[Any]] /* scalafix:ok DisableSyntax.asInstanceOf */

  inline def requireSingletons[T <: Tuple]: Unit =
    inline erasedValue[T] match
      case _: EmptyTuple     => ()
      case _: (head *: tail) =>
        type Head = head

        // ValueOf exists for singleton types only, which is exactly the condition we want to enforce.
        summonFrom {
          case _: ValueOf[Head] => ()
          case _                =>
            error(
              "Encoder.Primitive can only be derived for sums whose cases are all singletons; derive Encoder.Object instead"
            )
        }

        requireSingletons[tail]
