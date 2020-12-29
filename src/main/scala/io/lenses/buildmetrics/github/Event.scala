package io.lenses.buildmetrics.github

import java.time.Instant

import io.circe.generic.JsonCodec
import io.circe.Decoder
import cats.syntax.functor._

sealed trait GithubEvent {
  def `type`: String
}
object GithubEvent {
  @JsonCodec final case class Payload(ref: String, head: String)

  implicit val decoder: Decoder[GithubEvent] =
    Decoder[PushEvent].widen.or(Decoder[OtherEvent].widen)
}

@JsonCodec final case class PushEvent(
    id: String,
    `type`: String,
    `created_at`: Instant,
    payload: GithubEvent.Payload
) extends GithubEvent
object PushEvent {
  val Type = "PushEvent"
}

@JsonCodec final case class OtherEvent(`type`: String) extends GithubEvent
