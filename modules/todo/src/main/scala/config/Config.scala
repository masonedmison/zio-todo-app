package config

import zio.Task
import ciris._
import com.comcast.ip4s._
import scala.concurrent.duration._
import zio.interop.catz._

object Config {
  def load: Task[AppConfig] =
    default
      .load[Task]

  private def default: ConfigValue[Task, AppConfig] =
    env("TODO_DB_PASSWORD").as[String].secret.map { dbPass =>
      AppConfig(
        DbConfig(
          url = "jdbc:h2:./example;DB_CLOSE_DELAY=-1",
          driver = "org.h2.Driver",
          user = "",
          password = dbPass
        ),
        HttpServerConfig(
          host = host"localhost",
          port = port"8080",
          timeout = 10.seconds,
          idleTimeInPool = 30.seconds
        )
      )

    }
}
