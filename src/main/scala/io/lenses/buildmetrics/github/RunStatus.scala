package io.lenses.buildmetrics.github

object RunStatus {
  val Success = "success"
  val Failure = "failure"
  val Error = "error"

  val All = Set(Success, Failure, Error)
}
