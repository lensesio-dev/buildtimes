package io.lenses.buildmetrics.github

import io.circe.generic.JsonCodec

@JsonCodec final case class CommitStatuses(
    state: String,
    statuses: Vector[CommitStatus]
)
object CommitStatuses {
  val SuccessState = "success"
}
