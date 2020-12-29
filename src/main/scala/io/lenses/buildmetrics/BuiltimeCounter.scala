package io.lenses.buildmetrics

import io.lenses.buildmetrics.github._
import cats.implicits._
import cats.FlatMap
import java.time.Duration
import cats.effect.ConcurrentEffect
import cats.NonEmptyParallel
import cats.effect.Timer
import cats.effect.Sync
import java.time.Instant

trait BuiltimeCounter[F[_]] {
  def forCommit(
      owner: Owner,
      repo: Repo,
      sha: Sha1,
      commitPushedAt: Instant
  )(implicit
      sync: Sync[F],
      timer: Timer[F],
      ce: ConcurrentEffect[F],
      nep: NonEmptyParallel[F]
  ): F[Option[CheckRunDuration]]
}
object BuiltimeCounter {
  type RepoBranch = (Owner, Repo, Branch)
  type BranchStatusChecks = Map[RepoBranch, Set[String]]

  def apply[F[_]: FlatMap](
      githubClient: ApiClient[F]
  ): BuiltimeCounter[F] =
    new BuiltimeCounter[F] {
      override def forCommit(
          owner: Owner,
          repo: Repo,
          sha: Sha1,
          commitPushedAt: Instant
      )(implicit
          sync: Sync[F],
          timer: Timer[F],
          ce: ConcurrentEffect[F],
          nep: NonEmptyParallel[F]
      ) =
        (
          githubClient
            .branchProtectionFor(owner, repo, Branch("master"))
            .map(_.map(_.required_status_checks.contexts)),
          githubClient.checkRunsFor(owner, repo, sha),
          githubClient.statusesFor(owner, repo, sha)
        ).parMapN { case (maybeRequiredChecks, checkRuns, statuses) =>
          val requiredChecks = maybeRequiredChecks.getOrElse(Set.empty[String])
          val (successStatueses, failedStatuses) = commitStatusDurations(
            commitPushedAt,
            statuses.statuses.filter(s => requiredChecks.contains(s.context))
          )
          val (successChecks, failedChecks) =
            completedCheckRunDurations(
              checkRuns.filter(cr => requiredChecks.contains(cr.name))
            )

          val allSuccessDurations = successStatueses ++ successChecks
          val allFailedDurations = failedStatuses ++ failedChecks

          val allChecksPassed = requiredChecks
            .forall(check => allSuccessDurations.exists(_.check == check))

          if (requiredChecks.nonEmpty && allChecksPassed)
            allSuccessDurations.maxByOption(_.duration)
          else
            allFailedDurations.minByOption(_.duration)

        }
    }

  private def commitStatusDurations(
      commitPushedAt: Instant,
      statuses: Vector[CommitStatus]
  ): (Vector[CheckRunDuration], Vector[CheckRunDuration]) =
    statuses
      .collect {
        case s
            if s.state == RunStatus.Success || s.state == RunStatus.Failure =>
          CheckRunDuration(
            s.context,
            isSuccess = s.state == RunStatus.Success,
            Duration.ofMillis(
              s.updated_at
                .minusMillis(commitPushedAt.toEpochMilli())
                .toEpochMilli()
            )
          )
      }
      .partition(_.isSuccess)

  private def completedCheckRunDurations(
      checkRuns: Vector[CheckRun]
  ): (Vector[CheckRunDuration], Vector[CheckRunDuration]) =
    checkRuns
      .collect {
        case cr
            if cr.conclusion.contains(RunStatus.Success) || cr.conclusion
              .contains(
                RunStatus.Failure
              ) =>
          cr.completed_at.map { completed =>
            CheckRunDuration(
              cr.name,
              isSuccess = cr.conclusion.contains(RunStatus.Success),
              Duration.ofMillis(
                completed
                  .minusMillis(cr.started_at.toEpochMilli())
                  .toEpochMilli()
              )
            )
          }
      }
      .flatten
      .partition(_.isSuccess)

}
