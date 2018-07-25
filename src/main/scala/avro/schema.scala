package skeuomorph
package avro

import cats.Functor
import cats.data.NonEmptyList

sealed trait Order
object Order {
  case object Ascending  extends Order
  case object Descending extends Order
  case object Ignore     extends Order
}

case class Field[A](
    name: String,
    aliases: List[String],
    doc: Option[String],
    order: Option[Order],
    tpe: Schema[A]
)

sealed trait Schema[A]
object Schema {

  type TypeName = String

  /* primitive types */
  case class TNull[A]()                    extends Schema[A]
  case class TBoolean[A]()                 extends Schema[A]
  case class TInt[A]()                     extends Schema[A]
  case class TLong[A]()                    extends Schema[A]
  case class TFloat[A]()                   extends Schema[A]
  case class TDouble[A]()                  extends Schema[A]
  case class TBytes[A]()                   extends Schema[A]
  case class TString[A]()                  extends Schema[A]
  case class TNamedType[A](name: TypeName) extends Schema[A]
  case class TArray[A](item: A)            extends Schema[A]
  case class TMap[A](values: A)            extends Schema[A]
  case class TRecord[A](
      name: TypeName,
      namespace: Option[String],
      aliases: List[TypeName],
      doc: Option[String],
      fields: List[Field[A]])
      extends Schema[A]
  case class TEnum[A](
      name: TypeName,
      namespace: Option[String],
      aliases: List[TypeName],
      doc: Option[String],
      symbols: List[String],
      symbolLookup: Int => Option[String])
      extends Schema[A]
  case class TUnion[A](options: NonEmptyList[A], unionLookup: Int => Option[A])                       extends Schema[A]
  case class TFixed[A](name: TypeName, namespace: Option[String], aliases: List[TypeName], size: Int) extends Schema[A]
  implicit val typeFunctor: Functor[Schema] = new Functor[Schema] {
    def map[A, B](fa: Schema[A])(fn: A => B): Schema[B] = fa match {
      case Schema.TNull()          => Schema.TNull()
      case Schema.TBoolean()       => Schema.TBoolean()
      case Schema.TInt()           => Schema.TInt()
      case Schema.TLong()          => Schema.TLong()
      case Schema.TFloat()         => Schema.TFloat()
      case Schema.TDouble()        => Schema.TDouble()
      case Schema.TBytes()         => Schema.TBytes()
      case Schema.TString()        => Schema.TString()
      case Schema.TNamedType(name) => Schema.TNamedType(name)
      case Schema.TArray(item)     => Schema.TArray(fn(item))
      case Schema.TMap(values)     => Schema.TMap(fn(values))
      case Schema.TRecord(name, namespace, aliases, doc, fields) =>
        Schema.TRecord(name, namespace, aliases, doc, fields.map(field => field.copy(tpe = map(field.tpe)(fn))))
      case Schema.TEnum(name, namespace, aliases, doc, symbols, symbolLookup) =>
        Schema.TEnum(name, namespace, aliases, doc, symbols, symbolLookup)
      case Schema.TUnion(options, unionLookup)           => Schema.TUnion(options.map(fn), unionLookup.andThen(x => x.map(fn)))
      case Schema.TFixed(name, namespace, aliases, size) => Schema.TFixed(name, namespace, aliases, size)
    }
  }
}