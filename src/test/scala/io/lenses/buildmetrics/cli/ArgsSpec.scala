package io.lenses.buildmetrics.cli

import org.specs2.mutable.Specification
import caseapp.core.app.CaseApp
import io.lenses.buildmetrics.github.Owner
import io.lenses.buildmetrics.github.Repo
import io.lenses.buildmetrics.github.Token
import cats.data.NonEmptyList
import caseapp.cats.CatsArgParser._

class ArgsSpec extends Specification {
  "Args parser" >> {
    "parses a list of github owner/repos" >> {
      CaseApp.parse[Args](
        Seq(
          "--repo",
          "owner/repo1",
          "--repo",
          "other-owner/repo2",
          "--jdbc-url",
          "jdbc:postgresql:foo",
          "--github-creds",
          "testuser:some-token"
        )
      ) should_== Right(
        Args(
          NonEmptyList.of(
            OwnerRepo(Owner("owner"), Repo("repo1")),
            OwnerRepo(Owner("other-owner"), Repo("repo2"))
          ),
          ApiCreds("testuser", Token("some-token")),
          "jdbc:postgresql:foo"
        ) -> Nil
      )
    }
  }
}
