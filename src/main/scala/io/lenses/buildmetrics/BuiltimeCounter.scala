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
  ): F[Option[NamedDuration]]
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
          val allDurations = commitStatusDurations(
            commitPushedAt,
            statuses.statuses.filter(s => requiredChecks.contains(s.context))
          ) ++
            completedCheckRunDurations(
              checkRuns.filter(cr => requiredChecks.contains(cr.name))
            )

          val allChecksPassed = requiredChecks
            .forall(check => allDurations.exists(_.check == check))

          allDurations
            .maxByOption(_.duration)
            .filter(_ => allChecksPassed)
        }
    }

  private def commitStatusDurations(
      commitPushedAt: Instant,
      statuses: Vector[CommitStatus]
  ): Vector[NamedDuration] =
    statuses.collect {
      case s if s.state == "success" =>
        NamedDuration(
          s.context,
          Duration.ofMillis(
            s.updated_at
              .minusMillis(commitPushedAt.toEpochMilli())
              .toEpochMilli()
          )
        )
    }

  private def completedCheckRunDurations(
      checkRuns: Vector[CheckRun]
  ): Vector[NamedDuration] =
    checkRuns.collect {
      case cr if cr.conclusion == RunConclusion.Success =>
        cr.completed_at.map { completed =>
          NamedDuration(
            cr.name,
            Duration.ofMillis(
              completed.minusMillis(cr.started_at.toEpochMilli()).toEpochMilli()
            )
          )
        }
    }.flatten

}
