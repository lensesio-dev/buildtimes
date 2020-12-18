package io.lenses.github

import org.http4s.Request
import org.http4s.Uri
import org.http4s.Header
import org.http4s.Headers
import org.http4s.headers._
import org.http4s.circe._
import cats.effect.Sync
import org.http4s.EntityDecoder
import org.http4s.BasicCredentials
import cats.implicits._
import org.http4s.QueryParamDecoder
import cats.FlatMap
import org.http4s.dsl.impl.QueryParamDecoderMatcher

trait ApiClient[F[_]] {
  def pushEventsFor(owner: Owner, repo: Repo): F[Vector[PushEvent]]
  def statusesFor(commit: Sha1): F[CommitStatuses]
  def checkRunsFor(commit: Sha1): F[Vector[CheckRuns]]
}

object ApiClient {
  final case class Config(baseUri: Uri, userName: String, token: Token)
  def apply[F[_]: Sync: FlatMap](
      config: Config,
      httpClient: org.http4s.client.Client[F]
  ): ApiClient[F] =
    new ApiClient[F] {

      implicit val entityDec: EntityDecoder[F, Vector[GithubEvent]] =
        jsonOf[F, Vector[GithubEvent]]

      implicit val pageQueryParamDecoder: QueryParamDecoder[Page] =
        QueryParamDecoder[Int].map(Page(_))

      object PageQueryParamMatcher
          extends QueryParamDecoderMatcher[Page]("page")

      def maybePage(rel: String, link: Link): Option[Page] = for {
        link <- link.values.find(_.rel.contains(rel))
        page <- PageQueryParamMatcher.unapply(
          link.uri.query.multiParams
        )
      } yield page

      def withHeaders(req: Request[F]): Request[F] = req.withHeaders(
        Headers.of(
          Header("Accept", "application/vnd.github.v3+json"),
          Authorization(BasicCredentials(config.userName, config.token.value))
        )
      )

      override def pushEventsFor(
          owner: Owner,
          repo: Repo
      ) = {
        def fetchPage(
            page: Page,
            acc: Vector[PushEvent]
        ): F[Vector[PushEvent]] = {

          val request = withHeaders(
            Request[F](uri =
              (config.baseUri / "repos" / owner.value / repo.value / "events")
                .withQueryParam("page", page.toInt)
            )
          )

          httpClient
            .run(request)
            .use { resp =>
              val maybeNextAndLastPage =
                resp.headers
                  .find(_.name == Link.name)
                  .flatMap(Link.matchHeader)
                  .flatMap { link =>
                    (
                      maybePage("next", link),
                      maybePage("last", link)
                    ).tupled
                  }

              resp
                .as[Vector[GithubEvent]]
                .map(_.collect {
                  case e: PushEvent if e.`type` == PushEvent.Type => e
                })
                .flatMap { events =>
                  val allEvents = acc ++ events
                  maybeNextAndLastPage.fold(allEvents.pure[F]) {
                    case (next, last) =>
                      if (page == last) allEvents.pure[F]
                      else fetchPage(next, allEvents)
                  }
                }
            }
        }

        fetchPage(Page(1), Vector.empty)
      }

      override def statusesFor(commit: Sha1): F[CommitStatuses] = ???

      override def checkRunsFor(commit: Sha1): F[Vector[CheckRuns]] = ???

    }
}
