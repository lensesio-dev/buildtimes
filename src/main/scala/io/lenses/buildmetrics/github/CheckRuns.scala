package io.lenses.buildmetrics.github

import io.circe.generic.JsonCodec

@JsonCodec final case class CheckRuns(`check_runs`: Vector[CheckRun])
