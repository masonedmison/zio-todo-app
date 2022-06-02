package domain

import java.util.UUID
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.JsonCodec

@JsonCodec
case class Todo(title: String, order: Option[Int], completed: Boolean)

case class TodoEntry(id: UUID, todo: Todo)
object TodoEntry {
  implicit val forTodoEntryEncoder: Encoder[TodoEntry] = deriveEncoder
}

case class TodoModify(title: Option[String], order: Option[Int], completed: Option[Boolean])
object TodoModify {
  implicit val forTodoModifyDecoder: Decoder[TodoModify] = deriveDecoder
}
