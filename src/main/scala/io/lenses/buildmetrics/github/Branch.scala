package io.lenses.buildmetrics.github

final case class Branch(value: String) extends AnyVal
object Branch {
  def fromRef(ref: String): Branch = {
    Branch(ref.replace("refs/heads/", ""))
  }
}
