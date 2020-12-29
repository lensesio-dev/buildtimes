## Buildtimes

Simple CLI app to extract project build time metrics from Github and sink them into 
an RDBMS (i.e. currently Postgres).

### Development status

This project is not production ready and is still pre-alpha quality!

### Usage

```bash
 sbt 'run --repo org/repo1 \
          --repo org/repo2 \
	  --github-creds username:$GH_TOKEN\
	  --jdbc-url jdbc:postgresql://localhost/$DBNAME?user=pguser&password=$PGPASSWORD'
```
