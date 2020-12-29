package io.lenses.buildmetrics.cli

import io.lenses.buildmetrics.github.Owner
import io.lenses.buildmetrics.github.Repo
import caseapp.core.argparser.ArgParser
import caseapp.core.argparser.SimpleArgParser
import caseapp.core.Error

final case class OwnerRepo(owner: Owner, repo: Repo)
object OwnerRepo {
  implicit val cliParser: ArgParser[OwnerRepo] =
    SimpleArgParser.from[OwnerRepo]("OwnerRepo") { s =>
      s.split('/').toList match {
        case fst :: snd :: Nil if fst.nonEmpty && snd.nonEmpty =>
          Right(OwnerRepo(Owner(fst), Repo(snd)))
        case _ =>
          Left(Error.MalformedValue("owner-repo", s"Cannot parse from '$s'"))
      }
    }
}
