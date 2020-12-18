package io.lenses.buildmetrics

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import io.circe.parser.decode
import cats.syntax.either._
import io.lenses.github.PushEvent
import io.circe.Decoder
import io.lenses.github.GithubEvent
import io.lenses.github.CheckRun
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

object ParsingExample extends IOApp {

//   val json = "blah"
  override def run(args: List[String]): IO[ExitCode] = for {
    eventsJson <- readResource("events.json")
    checkRunsJson <- readResource("check-runs.json")
    statusJson <- readResource("status.json")
    pushEvents <- parsePushEvents(eventsJson)
    checkRuns <- parseCheckRuns(checkRunsJson)
    statuses <- parseCommitStatus(statusJson)

    _ = println(s"got ${pushEvents.size} push events")
    _ = println(s"got ${checkRuns.size} check runs")
    _ = println(s"got ${statuses.size} statuses")

    blockingPool = Executors.newFixedThreadPool(5)
    blocker = Blocker.liftExecutorService(blockingPool)
    pushEvents <- BlazeClientBuilder[IO](blocker.blockingContext).resource.use {
      httpClient =>
        val client = ApiClient(
          ApiClient.Config(
            Uri.unsafeFromString("https://api.github.com"),
            "afiore",
            Token("666f787ae7d7cd758287e786b9ead9de69e22633")
          ),
          httpClient
        )
        client.pushEventsFor(
          Owner("lensesio-dev"),
          Repo("lenses-backend")
        )
    }
    _ = println(s"got events: $pushEvents")

  } yield ExitCode.Success

  private def parseCheckRuns(json: String): IO[List[CheckRun]] =
    decodeIO[CheckRuns](json).map(_.check_runs)

  private def parsePushEvents(json: String): IO[List[PushEvent]] =
    decodeIO[List[GithubEvent]](json).map(_.collect {
      case ev: PushEvent if ev.`type` == PushEvent.Type => ev
    })

  private def parseCommitStatus(json: String): IO[List[CommitStatus]] =
    decodeIO[CommitStatuses](json).map(_.statuses)

  private def decodeIO[A: Decoder](json: String): IO[A] =
    IO.fromEither(decode[A](json).leftMap(_.fillInStackTrace()))

  private def readResource(name: String): IO[String] = IO {
    val is = getClass().getResourceAsStream(s"/$name")
    val s = new java.util.Scanner(is).useDelimiter("\\A")
    if (s.hasNext()) s.next() else ""
  }

}
