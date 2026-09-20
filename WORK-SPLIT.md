# Work split across the four members

Divided by layer, so each member owns whole files. Nobody has to explain code
somebody else wrote, and the boundaries follow the ones the course itself draws.

| Member | Owns |
|---|---|
| **Karan** (lead) | Domain model and persistence |
| **Yuvraj** | Database connectivity, JDBC and reporting |
| **Tia** | Servlets, session and security |
| **Chandra** | Presentation, validation and the business rules |

---

## Karan : Domain model and persistence

**Owns the object model and how it becomes tables.**

| File | Lines | What it is |
|---|---|---|
| `entity/AppUser.java` | 122 | Account and credential record |
| `entity/StudentProfile.java` | 120 | Academic record, `@OneToOne` to the account |
| `entity/Company.java` | 82 | Recruiter, `@OneToMany` to drives |
| `entity/Drive.java` | 161 | A drive and its eligibility rule set |
| `entity/JobApplication.java` | 107 | Join entity with a unique constraint |
| `entity/AuditLog.java` | 79 | Written by the interceptor |
| `entity/Role.java`, `ApplicationStatus.java`, `DriveStatus.java` | 56 | Enumerations |
| `resources/META-INF/persistence.xml` | 55 | The persistence unit, Hibernate named as provider |
| `ejb/DataSeeder.java` | 169 | `@Singleton @Startup` bean that fills an empty database |

**Be ready to explain**

- Why `@OneToOne`, `@OneToMany` and `@ManyToOne` sit where they do, and which
  side owns the foreign key.
- The unique constraint on `(student_id, drive_id)`: it is what stops a replayed
  POST creating a second application, and the check in Java is only the polite
  version of it.
- `FetchType.LAZY` versus `EAGER`, and why the lazy collections are never touched
  from a servlet: the entity is detached by then and would throw.
- Why `user_role` and `logged_at` are not called `role` and `at`. Both are
  reserved words in Derby, which the project ran on before MySQL.
- `hibernate.hbm2ddl.auto=update`, and why it is not `create-drop`.

---

## Yuvraj : Database connectivity, JDBC and reporting

**Owns everything about reaching the database, and the SQL half of the project.**

| File | Lines | What it is |
|---|---|---|
| `jdbc/PlacementReportDao.java` | 404 | The JDBC layer: pool lookup, PreparedStatement, ResultSet, transaction |
| `jdbc/DatabaseExplorerDao.java` | 330 | Schema and data browsing, and the guarded query console |
| `jdbc/BranchReport.java`, `CompanyReport.java`, `CandidateRow.java`, `ConnectionInfo.java`, `ResultTable.java`, `TableDetail.java` | 480 | Result carriers, deliberately not entities |
| `web/ReportsServlet.java`, `DatabaseExplorerServlet.java` | 195 | Controllers for the two database pages |
| `views/admin-reports.jsp`, `admin-database.jsp` | 640 | The numbers, the SQL, the schema and the live connection |
| `webapp/WEB-INF/glassfish-resources.xml` | 50 | The connection pool and the JNDI datasource |
| `test/jdbc/DatabaseConnectivityTest.java`, `QueryConsoleGuardTest.java` | 310 | Live database tests and the console guard tests |

**Be ready to explain**

- The five steps: get a `DataSource` from JNDI, get a `Connection`, prepare a
  statement, read the `ResultSet`, close everything. Point at each in the file.
- Why the pool exists: opening a TCP connection and authenticating per query
  would dominate the response time.
- Why nothing in the Java contains a JDBC URL or a password. The code asks for
  `java:app/jdbc/placementDS` and the container supplies the pool.
- Why the same datasource is consumed twice, by Hibernate through
  `persistence.xml` and directly by this DAO.
- `PreparedStatement` versus string concatenation. The search builds `?`
  placeholders and binds by index, so `' OR 1=1 --` is searched for as a branch
  name. There is a test that proves exactly this.
- The transaction in `closeDriveAndRejectPending`: `setAutoCommit(false)`, two
  updates, `commit()`, `rollback()` on failure, and why the `finally` block puts
  auto commit back before the connection returns to the pool.
- Why the bean is `NOT_SUPPORTED`: driving a transaction by hand only means
  something when the container is not already managing one.
- The Database page: where the tables, columns, keys and indexes come from, which
  is `DatabaseMetaData` and not anything written in the project.
- Why a table name is whitelisted rather than bound. An identifier cannot be a
  `?` parameter, so the name is checked against the list the database itself
  reported and the value used is the one the database gave back.
- The three layers guarding the query console: the statement check, the read only
  connection, and a database user with rights on one schema.
- Why the password digest is masked in the data access class rather than in the
  page: masking in a view leaves the value one careless page away from being
  printed.

---

## Tia : Servlets, session and security

**Owns the request path: who is asking, are they allowed, and where does it go.**

| File | Lines | What it is |
|---|---|---|
| `web/LoginServlet.java` | 154 | Sign in, session fixation defence, cookies |
| `web/LogoutServlet.java` | 43 | Invalidation and closing the stateful bean |
| `web/RegisterServlet.java` | 137 | Self registration |
| `security/AuthFilter.java` | 91 | The single gate in front of every signed in page |
| `security/PasswordHasher.java` | 87 | PBKDF2 with a per user salt |
| `ejb/AuthService.java` | 168 | Credential logic, lockout, the only class that compares a password |
| `ejb/AuthResult.java` | 66 | Outcome of an attempt |
| `web/SessionUser.java`, `SessionTracker.java`, `Web.java`, `ThemeServlet.java` | 222 | Session model, listener, CSRF, cookie theme |
| `web/BulkImportServlet.java` | 193 | Non blocking read with a `ReadListener` |
| `web/ResumeDownloadServlet.java` | 68 | File download with an ownership check |
| `test/security/PasswordHasherTest.java` | 80 | Hashing tests |

**Be ready to explain**

- The order of checks on sign in: CSRF token, then email shape, then the
  database. A malformed address never reaches a query.
- Why a wrong password and an unknown account give the same message and take the
  same time. `PasswordHasher.burn` exists only for that.
- `request.changeSessionId()`: what session fixation is and why this stops it.
- Salting: two people with the same password get different digests, and there is
  a test asserting it.
- The filter chain: no session bounces to login with the original URL remembered,
  `/admin/` additionally requires the officer role, POSTs must carry the token,
  and protected pages are `no-store` so Back after logout shows the login page.
- Why the filter is `asyncSupported`. Without it `startAsync()` in the import
  servlet is illegal, which is a real bug this project hit.
- `ReadListener` versus a blocking read, and what `isReady()` is protecting.
- Cookie versus session: the theme cookie survives sign out, the session does not.

---

## Chandra : Presentation, validation and the business rules

**Owns everything the user sees and every rule applied to what they type.**

| File | Lines | What it is |
|---|---|---|
| `views/*.jsp` (18 pages) | ~2300 | Every screen, JSTL and EL only |
| `views/layout-top.jspf`, `layout-bottom.jspf`, `icons.jspf` | 160 | Shared shell and the SVG sprite |
| `assets/app.css`, `app2.css` | 700 | The design system, two palettes |
| `util/Validators.java` | 99 | Every server side input rule |
| `util/Branches.java` | 30 | The branch whitelist |
| `util/UploadStore.java` | 83 | Where resumes go, and traversal defence |
| `ejb/EligibilityRules.java` | 61 | The eligibility policy as pure logic |
| `web/ProfileServlet.java` | 171 | Profile editing and the multipart upload |
| `test/util/*.java`, `test/ejb/EligibilityRulesTest.java` | 330 | Validation and rules tests |

**Be ready to explain**

- Why `scripting-invalid` is true in `web.xml`: no Java can be written into a
  page even by accident, so the views are JSTL and EL only.
- The MVC flow: servlet is the controller, the bean is the model, the JSP under
  `WEB-INF` is the view, and `RequestDispatcher.forward` is what keeps the JSP
  unreachable from a browser.
- Why every rule in `Validators` is checked on the server even though the form
  already has HTML5 attributes: a request built by hand skips all of them.
- The email pattern, and specifically why `admin@campus` and `a..b@x.com` are
  refused.
- Why `EligibilityRules` is a static method taking a date: it makes the rules
  testable with no database, and there are tests pinning both boundaries.
- `UploadStore.newFileName`: the stored name is generated, and only the extension
  survives from what the browser sent, because a name like `../../domain.xml` is
  a directory traversal.
- `jsp:useBean` in the footer and `c:redirect` in `index.jsp` as action elements.

---

## Shared, and honest about it

The build and run scripts (`build.ps1`, `run.ps1`, `test.ps1`, `stop.ps1`) and
the `ejb/` service beans other than the ones listed above were worked on
together. If asked, say so: `ApplicationService`, `DriveService`,
`StudentService`, `ShortlistBean`, `PortalStatsBean`, `NotificationSender`,
`NotificationMDB`, `AuditInterceptor` and `AuditWriter` sit on the boundary
between Karan's model and Tia's request handling, and were written by
whoever needed them first.
