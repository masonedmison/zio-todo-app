package http

import zio._
import repository.TodoRepository
import org.http4s.HttpRoutes
import zio.interop.catz._
import org.http4s.dsl.Http4sDsl
import org.http4s._
import org.http4s.circe._
import io.circe.Encoder
import domain.Todo
import io.circe.Decoder
import io.circe.syntax._
import domain.TodoModify
import domain.TodoEntry
import repository.DbError.TodoNotExists

class TodoRoutes[R <: TodoRepository]() {
  type TodoTask[A] = RIO[R, A]

  val dsl = Http4sDsl[TodoTask]
  import dsl._

  implicit def forEncoder[A](implicit ev: Encoder[A]): EntityEncoder[TodoTask, A] =
    jsonEncoderOf[TodoTask, A]
  implicit def forDecoder[A](implicit ev: Decoder[A]): EntityDecoder[TodoTask, A] =
    jsonOf[TodoTask, A]

  def routes: HttpRoutes[TodoTask] = HttpRoutes.of[TodoTask] {
    // view a todo
    case GET -> Root / UUIDVar(todoId) =>
      TodoRepository.get(todoId).flatMap(Ok(_))

    // create a todo
    case req @ POST -> Root =>
      req
        .asJsonDecode[Todo]
        .flatMap { todo =>
          Ok(TodoRepository.create(todo))

        }

    // modify a todo
    case req @ PATCH -> Root / UUIDVar(uid) =>
      req
        .asJsonDecode[TodoModify]
        .flatMap(td => Ok(TodoRepository.modify(uid, td)))
        .catchSome {
          case TodoNotExists(_) => BadRequest(s"Todo with $uid does not exist.")
        }

    // list all todos
    case GET -> Root =>
      val allTodos: TodoTask[fs2.Stream[TodoTask, TodoEntry]] = TodoRepository.getAll
      for {
        stream <- allTodos
        json   <- Ok(stream.map(_.asJson))
      } yield json

    // delete a single todo
    case DELETE -> Root / UUIDVar(uid) =>
      TodoRepository
        .delete(uid)
        .flatMap(Ok(_))
        .catchSome {
          case TodoNotExists(_) => BadRequest(s"Todo with $uid does not exist.")
        }

    // delete all todos
    case DELETE -> Root =>
      Ok(TodoRepository.deleteAll)
  }
}
