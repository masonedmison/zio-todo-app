package repository

import java.util.UUID

import zio._
import zio.macros.accessible
import fs2.Stream
import domain.{ Todo, TodoModify }
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import domain._
import zio.interop.catz._
import doobie.Fragments.setOpt

@accessible
trait TodoRepository {
  def get(id: UUID): Task[Option[Todo]]
  def create(todo: Todo): Task[Unit]
  def modify(id: UUID, todoMod: TodoModify): Task[Unit]
  def getAll: Stream[Task, TodoEntry]
  def delete(id: UUID): Task[Unit]
  def deleteAll: Task[Unit]
}

trait DbError extends Exception

object DbError {
  final case class TodoNotExists(msg: String) extends Exception
}

case class TodoRepositoryLive(xa: Transactor[Task], random: Random) extends TodoRepository {
  import TodoRepositoryLive.TodoSQL

  override def get(id: UUID): Task[Option[Todo]] =
    TodoSQL
      .get(id)
      .option
      .transact(xa)

  override def create(todo: Todo): Task[Unit] = Random.nextUUID.flatMap { uid =>
    TodoSQL
      .create(uid, todo)
      .run
      .transact(xa)
      .unit
  }

  override def modify(id: UUID, todoMod: TodoModify): Task[Unit] = {
    val completed = todoMod.completed.map(b => fr"completed = $b")
    val title     = todoMod.title.map(s => fr"title = $s")
    val order     = todoMod.order.map(i => fr"todoorder = $i")

    (fr"UPDATE Todos" ++ setOpt(completed, title, order) ++ fr"WHERE id = $id").update.run
      .transact(xa)
      .flatMap {
        case 0 => ZIO.fail(DbError.TodoNotExists("Item doesn't exist!"))
        case _ => ZIO.succeed(())
      }
  }

  override def delete(id: UUID): Task[Unit] =
    TodoSQL
      .delete(id)
      .run
      .transact(xa)
      .flatMap {
        case 0 => ZIO.fail(DbError.TodoNotExists("Item doesn't exist!"))
        case _ => ZIO.succeed(())
      }

  override def getAll: Stream[Task, TodoEntry] =
    TodoSQL.getAll.stream
      .transact(xa)

  override def deleteAll: Task[Unit] =
    TodoSQL.deleteAll.run
      .transact(xa)
      .unit

}

object TodoRepositoryLive {
  private[repository] object TodoSQL {
    def get(id: UUID): Query0[Todo] =
      sql"""
      SELECT title, todoorder, completed FROM Todos WHERE id = $id
      """.query[Todo]

    def getAll: Query0[TodoEntry] =
      sql"""
      SELECT id, title, todoorder, completed FROM Todos
      """.query[TodoEntry]

    def deleteAll: Update0 =
      sql"""
      DELETE FROM Todos 
      """.update

    def create(id: UUID, todo: Todo): Update0 =
      sql"""
      INSERT INTO Todos VALUES ($id, ${todo.title}, ${todo.order}, ${todo.completed})
      """.update

    def delete(id: UUID): Update0 =
      sql"""
      DELETE FROM Todos WHERE id = $id
      """.update

    def createTodoTable: Update0 =
      sql"""
        CREATE TABLE IF NOT EXISTS Todos (
          id   VARCHAR PRIMARY KEY,
          title VARCHAR NOT NULL,
          todoorder INTEGER DEFAULT 0,
          completed BOOLEAN NOT NULL  
        )
        """.update
  }

  private def createTodoTable: ZIO[Transactor[Task], Throwable, Unit] =
    for {
      xa <- ZIO.service[Transactor[Task]]
      _  <- TodoSQL.createTodoTable.run.transact(xa)
    } yield ()

  // Clearing tables just to try a scoped ZLayer
  val live: URLayer[Random with Transactor[Task], TodoRepository] =
    ZLayer.scoped {
      for {
        xa   <- ZIO.service[Transactor[Task]]
        rand <- ZIO.service[Random]
        todoRepo <- ZIO.acquireRelease {
          ZIO.succeed(TodoRepositoryLive(xa, rand)) <* createTodoTable.orDie.tap(
            _ => ZIO.logInfo("Creating Todo tables.")
          )
        } { td =>
          td.deleteAll.orDie.tap(_ => ZIO.logInfo("Deleting tables on finalization."))
        }
      } yield todoRepo
    }
}
