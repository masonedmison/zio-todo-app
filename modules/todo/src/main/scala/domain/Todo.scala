package domain

import java.util.UUID
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.JsonCodec
import io.circe.refined._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.types.numeric.PosInt

@JsonCodec
case class Todo(title: NonEmptyString, order: Option[PosInt], completed: Boolean)

case class TodoEntry(id: UUID, todo: Todo)
object TodoEntry {
  implicit val forTodoEntryEncoder: Encoder[TodoEntry] = deriveEncoder
}

case class TodoModify(title: Option[NonEmptyString], order: Option[PosInt], completed: Option[Boolean])
object TodoModify {
  implicit val forTodoModifyDecoder: Decoder[TodoModify] = deriveDecoder
}
