package io.lenses.buildmetrics.store

import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.lenses.buildmetrics.github.Repo
import io.lenses.buildmetrics.github.Owner

object Queries {
  def createBuiltTimesTable =
    sql"""
      CREATE TABLE IF NOT EXISTS buildtimes (
          owner text NOT NULL,
          repo text NOT NULL,
          branch text NOT NULL,
          commit text NOT NULL,
          context text NOT NULL,
          duration_ms bigint NOT NULL,
          created_at timestamp NOT NULL,
          event_id bigint NOT NULL,
          UNIQUE(owner, repo, commit)
      );

      CREATE UNIQUE INDEX IF NOT EXISTS buildtimes_owner_repo_commit ON buildtimes (owner, repo, commit);
      CREATE INDEX IF NOT EXISTS buildtimes_owner_repo ON buildtimes (owner, repo);
      CREATE INDEX IF NOT EXISTS buildtimes_event_id ON buildtimes (event_id);
        """.update

  val insertBuildTime = Update[BuildTime]("""
      INSERT INTO buildtimes (owner, repo, branch, commit, context, duration_ms, created_at, event_id)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     """)

  def latestEventId(owner: Owner, repo: Repo): Query0[Option[Long]] = {
    sql"""
     SELECT MAX(event_id) FROM buildtimes WHERE owner = $owner AND repo = $repo
  """.query[Option[Long]]
  }
}
