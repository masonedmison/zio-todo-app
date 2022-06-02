package config

import com.comcast.ip4s._
import ciris.Secret
import scala.concurrent.duration.FiniteDuration
import zio._

case class DbConfig(url: String, driver: String, user: String, password: Secret[String])
case class HttpServerConfig(host: Host, port: Port, timeout: FiniteDuration, idleTimeInPool: FiniteDuration)

case class AppConfig(db: DbConfig, http: HttpServerConfig)

object Configuration {
  type Configuration = DbConfig with HttpServerConfig
  val live: ULayer[Configuration] =
    ZLayer
      .fromZIO {
        Config.load.orDie
      }
      .flatMap { env =>
        val e = env.get[AppConfig]
        ZLayer.succeed(e.db) ++ ZLayer.succeed(e.http)
      }
}
