package io.lenses.buildmetrics.store

import org.specs2.mutable.Specification
import doobie.specs2.IOChecker
import doobie.util.transactor.Transactor
import cats.effect.IO
import cats.effect.ContextShift
import scala.concurrent.ExecutionContext
import doobie.implicits._
import io.lenses.buildmetrics.github.Owner
import io.lenses.buildmetrics.github.Repo

class QueriesSpec extends Specification with IOChecker {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:postgres",
    "lenses",
    "p0stgr3s"
  )

  check(Queries.createBuiltTimesTable)

  Queries.createBuiltTimesTable.run.transact(transactor).unsafeRunSync()
  check(Queries.insertBuildTime)
  check(Queries.latestEventId(Owner("x"), Repo("y")))
}
