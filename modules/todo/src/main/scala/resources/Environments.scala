package resources

import config.Configuration
import zio._
import zio.Random
import zio.Clock
import doobie.util.transactor.Transactor
import repository.TodoRepository
import repository.TodoRepositoryLive
import config.HttpServerConfig

object Environments {
  type HttpServerEnvironment = HttpServerConfig with Clock
  type AppEnvironment        = TodoRepository with HttpServerEnvironment

  val h2DbTransactor: ULayer[Transactor[Task]] =
    Configuration.live >>> MkH2.h2

  val todosRepository: ULayer[TodoRepository] =
    Random.live ++ h2DbTransactor >>> TodoRepositoryLive.live

  val httpServerEnvironment: ULayer[HttpServerEnvironment] =
    Configuration.live ++ Clock.live

  val appEnvironment: ULayer[AppEnvironment] =
    httpServerEnvironment ++ todosRepository
}
