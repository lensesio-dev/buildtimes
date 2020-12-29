package io.lenses.buildmetrics.cli

import io.lenses.buildmetrics.github.Token
import caseapp.core.argparser.ArgParser
import caseapp.core.argparser.SimpleArgParser
import caseapp.core.Error

final case class ApiCreds(username: String, token: Token)
object ApiCreds {
  implicit val cliParser: ArgParser[ApiCreds] =
    SimpleArgParser.from[ApiCreds]("ApiCreds") { s =>
      s.split(':').toList match {
        case fst :: snd :: Nil if fst.nonEmpty && snd.nonEmpty =>
          Right(ApiCreds(fst, Token(snd)))
        case _ =>
          Left(Error.MalformedValue("api-creds", s"Cannot parse from '$s'"))
      }
    }

}
