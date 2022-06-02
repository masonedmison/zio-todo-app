import doobie._
import java.util.UUID
import scala.util.Try
import cats.implicits._

package object domain extends OrphanInstances

trait OrphanInstances {
  implicit val forUUIDPut: Put[UUID] = Put[String].contramap(_.toString)
  implicit val forUUIDGet: Get[UUID] = Get[String].temap { s =>
    Try {
      UUID.fromString(s)
    }.toEither
      .leftMap(_.getMessage)
  }
}
