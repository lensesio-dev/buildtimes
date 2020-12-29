package io.lenses.buildmetrics.store

import cats.effect.ContextShift
import cats.effect.Async
import cats.effect.Blocker
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.lenses.buildmetrics.github.Owner
import io.lenses.buildmetrics.github.Repo
import java.sql.DriverManager
import cats.effect.Resource
import cats.implicits._

trait BuildtimeStore[F[_]] {
  def latestEventIdFor(owner: Owner, repo: Repo): F[Option[Long]]
  def write(builtTime: BuildTime): F[Unit]
}

object BuildtimeStore {
  final case class Config(dbUser: String, dbPassword: String, dbName: String)

  def resource[F[_]: Async: ContextShift](
      blocker: Blocker
  )(jdbcUrl: String): Resource[F, BuildtimeStore[F]] = for {
    connection <- Resource.make(
      Async[F].delay(DriverManager.getConnection(jdbcUrl))
    )(conn => Async[F].delay(conn.close()))
    xa = Transactor.fromConnection[F](connection, blocker)
    _ <- Resource.liftF(Queries.createBuiltTimesTable.run.transact[F](xa))

  } yield new BuildtimeStore[F] {
    override def write(builtTime: BuildTime): F[Unit] =
      Queries.insertBuildTime.run(builtTime).transact(xa).void

    override def latestEventIdFor(owner: Owner, repo: Repo) =
      Queries.latestEventId(owner, repo).unique.transact(xa)
  }

}
