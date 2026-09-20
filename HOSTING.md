# Getting this hosted

Target: containerize with the `Dockerfile` in this folder, deploy to Render or
Railway with a managed MySQL add-on. Not done yet — this is the procedure for
when you're ready to actually do it. Docker isn't installed in the sandbox
this was written in, so the image below is unverified; smoke-test it locally
(last section) before trusting it to a real deploy.

## Why this is more work than the other project

CampusConnect is a normal web app process. This is a full Jakarta EE server
(Payara/GlassFish) that reads its database connection from a JNDI resource
declared in `src/main/webapp/WEB-INF/glassfish-resources.xml`, and that file
currently hardcodes `localhost:3306` / `placement_user` / `Placement@2026` —
correct for your machine, wrong for a host. There's no envvar layer in front
of it the way a typical app reads `DATABASE_URL`.

## Procedure

**1. Point the app at the production database, then rebuild.**

In `src/main/webapp/WEB-INF/glassfish-resources.xml`, change the `URL`,
`user` and `password` properties on the `jdbc-connection-pool` to the
managed MySQL instance's host/port/db/credentials (Render and Railway both
give you these on the database's dashboard). Then:

```powershell
.\build.ps1
```

This produces a `dist/placement.war` built against the production DB config.
**Do not commit this rebuilt WAR or the edited file with real prod
credentials in it** — keep a local-only copy of the edited
`glassfish-resources.xml`, or better, keep the repo pointed at localhost and
only swap the file in your working tree right before this build step.

**2. Build the image.**

```powershell
docker build -t placement-cell .
```

**3. Push it somewhere the host can pull from** (GitHub Container Registry is
the natural choice since the repo is already on GitHub):

```powershell
docker tag placement-cell ghcr.io/capnkay/placement-cell:latest
docker push ghcr.io/capnkay/placement-cell:latest
```

**4. Provision the database on the host first**, get its connection details,
go back to step 1 with those details, rebuild, repush.

**5. Create the web service:**
- **Render:** New → Web Service → Existing Image → the `ghcr.io` image
  above. Set the port to `8080`. Add a MySQL instance separately (Render's
  managed Postgres is free-tier; MySQL usually means Railway or an external
  host like PlanetScale/Aiven instead — check what's actually free right now
  before committing to Render for the DB half).
- **Railway:** New Project → Deploy from Docker Image → same image. Railway
  does offer a MySQL plugin directly in the same project, which is simpler
  than Render for this app specifically.

**6. First boot seeds itself** — `DataSeeder` (a `@Singleton @Startup` EJB)
fills an empty database with the same demo accounts as local
(`tpo@campus.edu`, `aarti.deshpande@campus.edu`, password `Campus@2026`), so
there's no separate seed step on the host.

## Smoke-testing the image locally first

```powershell
docker compose up --build
```

This starts MySQL 8 and the app together with the pool pointed at the
compose network (see the note at the top of `docker-compose.yml` — the WAR
you build for this test has to say `mysql`, not `localhost`, in its JDBC
URL). Visit `http://localhost:8080/placement/`. Tear down with
`docker compose down -v` when you're done — `-v` also drops the throwaway
DB volume.
