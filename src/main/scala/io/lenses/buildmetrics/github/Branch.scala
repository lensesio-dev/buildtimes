package io.lenses.buildmetrics.github

final case class Branch(value: String) extends AnyVal {
  def jiraId: Option[String] = value match {
    case Branch.JiraIdRegex(team, id) => Some(s"$team-$id")
    case _                            => None
  }
}
object Branch {
  val JiraIdRegex = raw"([A-Z]+)-(\d+)".r.unanchored
  def fromRef(ref: String): Branch = {
    Branch(ref.replace("refs/heads/", ""))
  }
}
