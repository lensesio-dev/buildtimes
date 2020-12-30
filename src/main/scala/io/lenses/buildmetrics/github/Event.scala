package io.lenses.buildmetrics.github

import java.time.Instant

import io.circe.generic.JsonCodec
import io.circe.Decoder
import cats.syntax.functor._
import io.lenses.buildmetrics.github.GithubEvent._

sealed trait GithubEvent extends Product with Serializable {
  def id: String
  def asPushEvent: Option[PushEvent]
}
object GithubEvent {
  @JsonCodec final case class PushPayload(ref: String, head: String)
  @JsonCodec final case class PullRequestHead(sha: String, ref: String)
  @JsonCodec final case class PullRequest(head: PullRequestHead)
  @JsonCodec final case class PullRequestPayload(pull_request: PullRequest)

  implicit val decoder: Decoder[GithubEvent] =
    List[Decoder[GithubEvent]](
      Decoder[PullRequestEvent].widen,
      Decoder[PushEvent].widen,
      Decoder[OtherEvent].widen
    ).reduce(_ or _)
}

@JsonCodec final case class PullRequestEvent(
    id: String,
    payload: PullRequestPayload,
    created_at: Instant
) extends GithubEvent {
  override def asPushEvent: Option[PushEvent] = None
  // def asPushEvent: Option[PushEvent] = Some(
  //   PushEvent(
  //     id,
  //     created_at,
  //     PushPayload(payload.pull_request.head.ref, payload.pull_request.head.sha)
  //   )
  // )
}

@JsonCodec final case class PushEvent(
    id: String,
    `created_at`: Instant,
    payload: GithubEvent.PushPayload
) extends GithubEvent {
  def asPushEvent: Option[PushEvent] = Some(this)
}

@JsonCodec final case class OtherEvent(id: String) extends GithubEvent {
  override def asPushEvent: Option[PushEvent] = None
}
