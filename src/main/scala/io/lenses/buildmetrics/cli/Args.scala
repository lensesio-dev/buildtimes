package io.lenses.buildmetrics.cli

import caseapp._
import _root_.cats.data.NonEmptyList

@AppName("buildmetrics")
@AppVersion("0.1.0")
@ProgName("buildmetrics")
final case class Args(
    @HelpMessage(
      "one or several Github repos (e.g. `lensesio-dev/lenses-core`)"
    )
    @ValueDescription("owner/repo")
    repo: NonEmptyList[OwnerRepo],
    @HelpMessage(
      "Github API credentials"
    )
    @ValueDescription("username:token")
    githubCreds: ApiCreds,
    @HelpMessage("a jdbc URL")
    @ValueDescription("jdbc:postgresql://host:port/database")
    jdbcUrl: String
)
