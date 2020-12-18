package io.lenses.github

import io.circe.generic.JsonCodec

@JsonCodec final case class CommitStatuses(
    state: String,
    statuses: List[CommitStatus]
)
object CommitStatuses {
  val SuccessState = "success"
}
