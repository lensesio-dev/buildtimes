package io.lenses.github

import java.time.Instant
import io.circe.generic.JsonCodec

@JsonCodec final case class CheckRun(
    started_at: Instant,
    completed_at: Instant,
    name: String,
    status: String,
    conclusion: RunConclusion
)
