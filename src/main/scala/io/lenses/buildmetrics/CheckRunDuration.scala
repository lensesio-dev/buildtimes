package io.lenses.buildmetrics

import java.time.Duration

final case class CheckRunDuration(
    check: String,
    isSuccess: Boolean,
    duration: Duration
)
