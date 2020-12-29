package io.lenses.buildmetrics.github

import io.circe.generic.JsonCodec
import java.time.Instant

@JsonCodec final case class CommitStatus(
    updated_at: Instant,
    context: String,
    state: String
)
