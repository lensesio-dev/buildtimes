package io.lenses.buildmetrics

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import io.lenses.github.ApiClient
import io.lenses.github.Token
import cats.effect.Blocker
import java.util.concurrent._
import org.http4s.Uri
import io.lenses.github.Owner
import io.lenses.github.Repo
import org.http4s.client.blaze.BlazeClientBuilder
import io.lenses.github.PushEvent
import fs2.Stream

object ParsingExample extends IOApp {

  val owner = Owner("lensesio-dev")
  val repo = Repo("lenses-backend")

  val blocker: Stream[IO, Blocker] =
    Stream
      .bracket(IO(Executors.newFixedThreadPool(5)))(ex => IO(ex.shutdown()))
      .map(Blocker.liftExecutorService)

  def pushEvents(blocker: Blocker): Stream[IO, PushEvent] = for {
    httpClient <- BlazeClientBuilder[IO](blocker.blockingContext).stream
    apiClient = ApiClient[IO](
      ApiClient.Config(
        Uri.unsafeFromString("https://api.github.com"),
        "afiore",
        Token("666f787ae7d7cd758287e786b9ead9de69e22633")
      ),
      httpClient
    )
    pushEvent <- apiClient.pushEventsFor(owner, repo)
  } yield pushEvent

  override def run(args: List[String]): IO[ExitCode] =
    blocker
      .flatMap(pushEvents)
      .evalTap(e => IO(println(s"push event: ${e}")))
      .compile
      .drain
      .as(ExitCode.Success)

  //     client
  //       .pushEventsFor(
  //         Owner("lensesio-dev"),
  //         Repo("lenses-backend")
  //       )
  //       .flatMap { events =>
  //         val fst = events.head.payload.head
  //         (
  //           client.checkRunsFor(
  //             Owner("lensesio-dev"),
  //             Repo("lenses-backend"),
  //             Sha1(fst)
  //           ),
  //           client.statusesFor(
  //             Owner("lensesio-dev"),
  //             Repo("lenses-backend"),
  //             Sha1(fst)
  //           )
  //         ).mapN { case (runChecks, statuses) =>
  //           (events, runChecks, statuses)
  //         }
  //       }
  // }
  // (pushEvents, checkRuns, statuses) = all
  // _ = println(s"got events: ${pushEvents.head} + ${pushEvents.size - 1} ...")
  // _ = println(s"got check runs: $checkRuns ...")
  // _ = println(s"got statuses: $statuses ...")

}
