package io.lenses.buildmetrics

import cats.effect.{ExitCode, IO}
import caseapp.cats.IOCaseApp
import io.lenses.buildmetrics.cli.Args
import caseapp.core.RemainingArgs
import caseapp.cats.CatsArgParser._
import io.lenses.buildmetrics.cli.OwnerRepo
import io.lenses.buildmetrics.github.ApiClient
import io.lenses.buildmetrics.store.BuildtimeStore
import fs2.Stream
import java.util.concurrent.Executors
import cats.effect.Blocker
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.Uri
import org.http4s.client.middleware._
import io.lenses.buildmetrics.github.Sha1
import io.lenses.buildmetrics.github.Branch
import io.lenses.buildmetrics.store.BuildTime
import java.time.Instant

object Main extends IOCaseApp[Args] {
  def run(args: Args, remaining: RemainingArgs) = (for {
    b <- blocker
    httpClient <- BlazeClientBuilder[IO](b.blockingContext).stream
    apiClient = ApiClient[IO](
      ApiClient.Config(
        Uri.unsafeFromString("https://api.github.com"),
        args.githubCreds.username,
        args.githubCreds.token
      ),
      ResponseLogger(true, true)(RequestLogger(true, true)(httpClient))
    )
    builtimeCounter = BuiltimeCounter(apiClient)
    store <- Stream.resource(
      BuildtimeStore.resource[IO](b)(
        args.jdbcUrl
      )
    )
    _ <- Stream
      .emits(args.repo.toList)
      .map(fetchBuildTimesFor(apiClient, builtimeCounter, store))
      .parJoinUnbounded

  } yield ()).compile.drain.as(ExitCode.Success)

  private val blocker: Stream[IO, Blocker] =
    Stream
      .bracket(IO(Executors.newFixedThreadPool(5)))(ex => IO(ex.shutdown()))
      .map(Blocker.liftExecutorService)

  private def fetchBuildTimesFor(
      apiClient: ApiClient[IO],
      builtimeCounter: BuiltimeCounter[IO],
      store: BuildtimeStore[IO]
  )(
      ownerRepo: OwnerRepo
  ): Stream[IO, Unit] = {
    val OwnerRepo(owner, repo) = ownerRepo
    Stream.eval(store.latestEventIdFor(owner, repo)).flatMap {
      maybeLastEventId =>
        apiClient
          .pushEventsFor(owner, repo)
          //In order to prevent persisting the same event more than once,
          //terminate stream as soon as the last stored event is encountered
          .takeWhile(event =>
            maybeLastEventId.fold(true)(_.toString != event.id)
          )
          .evalMap { event =>
            val branch = Branch.fromRef(event.payload.ref)
            val sha1 = Sha1(event.payload.head)
            builtimeCounter
              .forCommit(
                owner,
                repo,
                sha1,
                event.created_at
              )
              .flatMap {
                _.fold(IO.unit) {
                  case CheckRunDuration(ciCheck, isSuccess, duration) =>
                    store.write(
                      BuildTime(
                        owner,
                        repo,
                        branch,
                        sha1,
                        isSuccess,
                        ciCheck,
                        duration.toMillis(),
                        Instant.now(),
                        event.id.toLong
                      )
                    )
                }
              }
          }
    }
  }
}
