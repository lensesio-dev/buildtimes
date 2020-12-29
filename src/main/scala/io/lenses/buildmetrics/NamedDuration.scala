package io.lenses.buildmetrics

import java.time.Duration

final case class NamedDuration(check: String, duration: Duration)
