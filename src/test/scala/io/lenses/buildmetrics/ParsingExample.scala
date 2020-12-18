package io.lenses.buildmetrics

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import cats.implicits._
import io.circe.Decoder
import io.lenses.github.GithubEvent
import io.lenses.github.CheckRuns
import io.lenses.github.CommitStatus
import io.lenses.github.CommitStatuses
import io.lenses.github.ApiClient
import io.lenses.github.Token
import cats.effect.Blocker
import java.util.concurrent._
import org.http4s.Uri
import io.lenses.github.Owner
import io.lenses.github.Repo
import org.http4s.client.blaze.BlazeClientBuilder
import io.lenses.github.Sha1

object ParsingExample extends IOApp {

  val blockingPool = Executors.newFixedThreadPool(5)

  val blocker = Blocker.liftExecutorService(blockingPool)
  override def run(args: List[String]): IO[ExitCode] = for {
    all < BlazeClientBuilder[IO](blocker.blockingContext).resource.use {
      httpClient =>
        val client = ApiClient(
          ApiClient.Config(
            Uri.unsafeFromString("https://api.github.com"),
            "afiore",
            Token("666f787ae7d7cd758287e786b9ead9de69e22633")
          ),
          httpClient
        )

        client
          .pushEventsFor(
            Owner("lensesio-dev"),
            Repo("lenses-backend")
          )
          .flatMap { events =>
            val fst = events.head.payload.head
            (
              client.checkRunsFor(
                Owner("lensesio-dev"),
                Repo("lenses-backend"),
                Sha1(fst)
              ),
              client.statusesFor(
                Owner("lensesio-dev"),
                Repo("lenses-backend"),
                Sha1(fst)
              )
            ).mapN { case (runChecks, statuses) =>
              (events, runChecks, statuses)
            }
          }
    }
    (pushEvents, checkRuns, statuses) = all
    _ = println(s"got events: ${pushEvents.head} + ${pushEvents.size - 1} ...")
    _ = println(s"got check runs: $checkRuns ...")
    _ = println(s"got statuses: $statuses ...")

  } yield ExitCode.Success
}
