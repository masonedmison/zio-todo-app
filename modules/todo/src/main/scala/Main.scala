import zio._
import resources.Environments._
import http.TodoRoutes
import org.http4s.ember.server.EmberServerBuilder
import zio.interop.catz._
import config.HttpServerConfig
import cats.data.Kleisli
import org.http4s._
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import zio.logging.backend.SLF4J
import zio.logging._
import org.http4s.server.middleware.ResponseLogger
import org.http4s.server.middleware.RequestLogger

object Main extends ZIOAppDefault {

  type ServerRIO[A] = RIO[AppEnvironment, A]
  type ServerRoutes =
    Kleisli[ServerRIO, Request[ServerRIO], Response[ServerRIO]]

  val sl4jLayer = SLF4J.slf4j(
    logLevel = LogLevel.All,
    format = LogFormat.colored + LogFormat.enclosingClass
  ) >>> removeDefaultLoggers

  val loggers: HttpApp[ServerRIO] => HttpApp[ServerRIO] = { (http: HttpApp[ServerRIO]) =>
    RequestLogger.httpApp(true, true)(http)
  } andThen { (http: HttpApp[ServerRIO]) =>
    ResponseLogger.httpApp(true, true)(http)
  }

  def showEmberBanner(s: Server): Task[Unit] =
    ZIO.logInfo(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  def runServer(app: ServerRoutes): RIO[AppEnvironment with Scope, Unit] = {
    ZIO.serviceWithZIO[HttpServerConfig] { env =>
      EmberServerBuilder
        .default[ServerRIO]
        .withHost(env.host)
        .withPort(env.port)
        .withHttpApp(app)
        .build
        .toScopedZIO
        .tap(showEmberBanner)
        .orDie *> ZIO.never
    }
  }

  def createRoutes: ServerRoutes =
    loggers(
      new TodoRoutes[AppEnvironment].routes.orNotFound
    )

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = {
    ZIO
      .scoped {
        runServer(createRoutes)
      }
      .provide(Scope.default >>> appEnvironment, sl4jLayer)
  }

}
