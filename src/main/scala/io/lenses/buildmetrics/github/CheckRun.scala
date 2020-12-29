package io.lenses.buildmetrics.github

import java.time.Instant
import io.circe.generic.JsonCodec

@JsonCodec final case class CheckRun(
    started_at: Instant,
    completed_at: Option[Instant],
    name: String,
    status: String,
    conclusion: RunConclusion
)
