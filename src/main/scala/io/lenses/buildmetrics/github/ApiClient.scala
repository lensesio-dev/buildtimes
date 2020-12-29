package io.lenses.buildmetrics.github

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
import fs2.Chunk
import fs2.Stream

trait ApiClient[F[_]] {
  def pushEventsFor(owner: Owner, repo: Repo): Stream[F, PushEvent]
  def branchProtectionFor(
      owner: Owner,
      repo: Repo,
      branch: Branch
  ): F[Option[BranchProtection]]
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

        type State = (Chunk[PushEvent], Option[Page])

        def fetchPage(
            page: Option[Page]
        ): F[Option[State]] =
          page.fold(Option.empty[State].pure[F]) { page =>
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
                  .map { allEvents =>
                    val chunk = Chunk(allEvents.collect {
                      case e: PushEvent if e.`type` == PushEvent.Type => e
                    }: _*)

                    maybeNextAndLastPage
                      .filterNot { case (_, lastPage) => page == lastPage }
                      .fold((chunk, Option.empty[Page]).some) {
                        case (next, _) => (chunk, next.some).some
                      }
                  }
              }
          }
        Stream.unfoldChunkEval[F, Option[Page], PushEvent](Page(1).some) {
          maybePage =>
            fetchPage(maybePage)
        }
      }

      override def branchProtectionFor(
          owner: Owner,
          repo: Repo,
          branch: Branch
      ) = {
        implicit val entityDec: EntityDecoder[F, BranchProtection] =
          jsonOf[F, BranchProtection]

        val request = withHeaders(
          Request(uri =
            config.baseUri / "repos" / owner.value / repo.value / "branches" / branch.value / "protection"
          )
        )

        httpClient.expectOption[BranchProtection](request)
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
