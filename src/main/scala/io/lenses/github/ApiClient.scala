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
  def statusesFor(owner: Owner, repo: Repo, commit: Sha1): F[CommitStatuses]
  def checkRunsFor(owner: Owner, repo: Repo, commit: Sha1): F[Vector[CheckRun]]
}

object ApiClient {
  final case class Config(baseUri: Uri, userName: String, token: Token)
  def apply[F[_]: Sync: FlatMap](
      config: Config,
      httpClient: org.http4s.client.Client[F]
  ): ApiClient[F] =
    new ApiClient[F] {

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
        implicit val entityDec: EntityDecoder[F, Vector[GithubEvent]] =
          jsonOf[F, Vector[GithubEvent]]

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

      override def statusesFor(
          owner: Owner,
          repo: Repo,
          commit: Sha1
      ): F[CommitStatuses] = {

        implicit val entityDec: EntityDecoder[F, CommitStatuses] =
          jsonOf[F, CommitStatuses]

        val request = withHeaders(
          Request(uri =
            config.baseUri / "repos" / owner.value / repo.value / "commits" / commit.value / "status"
          )
        )

        httpClient.expect[CommitStatuses](request)
      }

      override def checkRunsFor(
          owner: Owner,
          repo: Repo,
          commit: Sha1
      ) = {
        implicit val entityDec: EntityDecoder[F, CheckRuns] =
          jsonOf[F, CheckRuns]

        val request = withHeaders(
          Request(uri =
            config.baseUri / "repos" / owner.value / repo.value / "commits" / commit.value / "check-runs"
          )
        )

        httpClient.expect[CheckRuns](request).map(_.check_runs)
      }

    }
}
