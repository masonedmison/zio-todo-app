package resources

import zio._
import doobie.util.transactor.Transactor
import zio.interop.catz._
import config.DbConfig

object MkH2 {
  val h2: ZLayer[DbConfig, Nothing, Transactor[Task]] =
    ZLayer.fromZIO {
      for {
        db <- ZIO.service[DbConfig]
      } yield Transactor.fromDriverManager(
        db.driver,
        db.url,
        db.user,
        db.password.value
      )
    }
}
