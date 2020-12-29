package io.lenses.buildmetrics.github

import io.circe.generic.JsonCodec

@JsonCodec final case class BranchProtection(
    required_status_checks: BranchProtection.RequiredStatusChecks
)
object BranchProtection {
  @JsonCodec final case class RequiredStatusChecks(contexts: Set[String])
}
