package io.lenses.github

import io.circe.generic.JsonCodec

@JsonCodec final case class CheckRuns(`check_runs`: List[CheckRun])
