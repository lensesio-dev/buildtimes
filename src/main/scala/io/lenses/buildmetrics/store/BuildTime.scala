package io.lenses.buildmetrics.store

import io.lenses.buildmetrics.github._
import java.time.Instant

final case class BuildTime(
    owner: Owner,
    repo: Repo,
    branch: Branch,
    commit: Sha1,
    isSuccess: Boolean,
    context: String,
    durationMs: Long,
    createdAt: Instant,
    eventId: Long,
    jiraId: Option[String]
)
