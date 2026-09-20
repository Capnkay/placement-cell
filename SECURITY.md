# Security and code quality review

A review of the whole application, the findings, what was changed, and what was
deliberately left alone with the reason. Written so that the accepted risks are
on the record rather than discovered by somebody else later.

Everything below is in the code now unless it says otherwise.

---

## 1. Findings that were fixed

### S1 : the sign in page printed working credentials
**Was:** the login page listed the seeded accounts and their shared password to
every anonymous visitor.
**Now:** removed. The demo credentials live in `README.md` and in the run script
output, where somebody setting the project up is already looking.
**Why it mattered:** a sign in page that hands out a username and password is a
sign in page with no sign in on it.

### S2 : protective headers only covered signed in pages
**Was:** `AuthFilter` set the cache and framing headers, and it only runs on
`/app/` and `/admin/`. The login page, the register page and the error pages got
nothing.
**Now:** `SecurityHeadersFilter` on `/*` sets them for every response, and
`AuthFilter` is left to do only access control.
**Why it mattered:** the login page is exactly the page an attacker would rather
was framable and cacheable.

### S3 : no content security policy
**Now:** `script-src 'self'` with no `'unsafe-inline'`. The browser refuses to
run any script that did not come from this application.
**What it cost:** every inline `<script>` and every `onsubmit="return confirm(…)"`
had to go. Confirmations are now `data-confirm` attributes wired up by
`assets/app.js`, and the bulk import script moved into the same file.
**Why it mattered:** this is the control that turns a stored cross site scripting
hole from a compromise into a piece of inert text sitting in a page.

### S4 : uploads were trusted on their extension
**Was:** a file was accepted because its name ended in `.pdf`.
**Now:** the first bytes are read before anything is written and compared
against the signature the format must start with (`%PDF`, `PK\x03\x04`, the OLE
header). A renamed file is refused.
**Tested by:** `UploadStoreTest`.

### S5 : the new database view could show password digests
**Was:** a data browser that selects from any table would have printed the
`password_hash` column.
**Now:** masked inside `DatabaseExplorerDao`, by column name, on the way out of
the result set. It applies to the data browser and to the console, so
`SELECT email, password_hash FROM app_user` returns the emails and the word
hidden.
**Why it is done there:** masking in the JSP would leave the value in memory and
one careless page away from being printed. Masking in the data access class means
no view in this application is capable of showing one.

### S6 : the query console is the largest new attack surface
It runs SQL that a person typed, so it is guarded three deep:

1. **The statement is checked.** One statement only, no semicolons, must begin
   with `SELECT`, `WITH`, `SHOW`, `DESCRIBE` or `EXPLAIN`, and is refused if it
   contains any keyword that writes, locks, or redirects output to a file.
2. **The connection is set read only** before the statement runs, so the server
   itself refuses a write even if the check above were fooled.
3. **The database user is granted rights on `placement_cell` and nothing else**,
   so even a successful write could not reach another schema.

Also capped at 200 rows and cancelled after 5 seconds, so a careless cross join
cannot take the application down. Verified live: `DROP TABLE`, `UPDATE`,
`SELECT 1; DROP TABLE drive` and `INTO OUTFILE` are all refused.
**Tested by:** `QueryConsoleGuardTest`.

### S7 : identifiers cannot be bound, so table names are whitelisted
A table name cannot be a `?` parameter, and the data browser needs one. Every
name is checked against the list `DatabaseMetaData` reports and the value used in
the SQL is the one **the database gave back**, never the one from the request.

### S8 : the audit trail recorded passwords
**Was:** the interceptor logged the arguments of every audited method, and
`AuthService.authenticate(email, password)` is an audited method.
**Now:** parameter names are read from the class file (the build passes
`-parameters`) and anything matching password, secret, token or credential is
replaced with `[redacted]`. The demo database was rebuilt so no pre-fix rows
survive. Confirmed: 0 rows in `audit_log` contain a password.

### S9 : the singleton bean still pointed at the old datasource
`PortalStatsBean` was reading `java:comp/DefaultDataSource`, the server's built
in Derby pool, after the application had moved to MySQL. It now uses
`java:app/jdbc/placementDS` like everything else.

---

## 2. Controls that were already in place

Recorded here because a reviewer should be able to find them, not because they
changed in this pass.

| Control | Where |
|---|---|
| Passwords stored as salted PBKDF2, 120 000 iterations | `PasswordHasher` |
| Unknown account and wrong password are indistinguishable, including in timing | `AuthService`, `PasswordHasher.burn` |
| Five failures lock an account for fifteen minutes | `AuthService` |
| Session id regenerated on sign in | `LoginServlet`, `request.changeSessionId()` |
| CSRF token on every state changing POST | `Web`, `AuthFilter` |
| Role check on every `/admin/` request | `AuthFilter` |
| Open redirect refused after sign in | `LoginServlet.safeNext` |
| Directory traversal refused on stored file names | `UploadStore.resolve` |
| A student can download only their own resume | `ResumeDownloadServlet` |
| Every query parameterised | throughout, and `DatabaseConnectivityTest` proves it |
| Output escaped with `c:out`, scriptlets disabled in `web.xml` | every JSP |
| Error pages show a class name, never a stack trace | `error-500.jsp` |

---

## 3. Accepted, with the reason

These are known and deliberate. Each would be wrong in a system holding real
student records, and each is listed so nobody has to guess whether it was noticed.

| # | Accepted | Why it is acceptable here | What production would do |
|---|---|---|---|
| A1 | The datasource password sits in plain text in `glassfish-resources.xml` | It is a local database created for this project, granted on one schema | A password alias in the server, or a secret from the environment |
| A2 | The session cookie is not marked `secure` | The project runs over plain HTTP on localhost, and a `secure` cookie would never be sent | HTTPS, `secure` cookies, and HSTS |
| A3 | No `Strict-Transport-Security` header | Sending it over HTTP would tell the browser to refuse HTTP to this host for months | Sent, once there is TLS |
| A4 | `style-src` allows inline styles | A handful of one off layout values are style attributes. An injected style can deface a page, not run code | Move the remaining inline styles into classes and drop `'unsafe-inline'` |
| A5 | A SQL console exists at all | Officer only, read only, three layers of guard, and it is the clearest way to show database connectivity | It would not ship |
| A6 | Every seeded account shares one password | Demo data, and the seeder only runs on an empty database | Per account passwords, forced change at first sign in |
| A7 | The keyword blocklist has false positives | `SELECT * FROM drive WHERE description LIKE '%create%'` is refused. Refusing a legitimate read is a much cheaper mistake than allowing a write | A real SQL parser rather than a regular expression |
| A8 | The audit trail is never pruned | It is a teaching artifact and the data is small | A retention policy |

---

## 4. Code quality changes made in the same pass

| Change | Reason |
|---|---|
| Eligibility rules moved out of `DriveService` into `EligibilityRules` | The rules were untestable while they sat behind an `EntityManager`. As a static method taking the date, both boundaries are now pinned by tests |
| `Audited.java` deleted | An interceptor binding annotation that nothing used, beside the `@Interceptors` attachment that everything actually uses. Two mechanisms for one job is a question waiting to be asked |
| Header setting moved out of `AuthFilter` | It was doing two jobs, and the second one silently did not cover the pages that needed it most |
| All inline JavaScript moved to `assets/app.js` | Required for the content security policy, and it means the pages contain markup rather than behaviour |
| `ResultSet` copied into `ResultTable` before returning | Handing a result set out of a data access method keeps its connection open. Enough of those and the pool is exhausted |
| Result values truncated at 200 characters | One large column should not be able to make a page unusable |

### Defects found by running it, not by reading it

| Defect | Cause |
|---|---|
| Deployment failed with `placementDS not found` | GlassFish scopes application resources to `java:app/`, so the name has to say so |
| `ClassNotFoundException` on the MySQL driver | It was in `domain1/lib/ext`, which relies on the JVM extension mechanism Java 9 removed |
| The database page returned 500 | `${rows.empty}` is unparseable: `empty` is a reserved word in the expression language |
| Separators rendered with no spacing | The rule was scoped to `.footer` only |
| The search checkbox was stretched to 130px | A `.filter-bar input` rule caught checkboxes as well as text fields |

---

## 5. How to check any of this yourself

```powershell
.\test.ps1     # 126 tests, including the console guard and the live database
```

In the running application, signed in as the placement officer:

- **Database → Query console**, try `DROP TABLE student_profile`. Refused, with
  the reason.
- Try `SELECT email, password_hash FROM app_user`. It runs, and the digest column
  comes back as hidden.
- **Audit**, search for `authenticate`. The password argument reads `[redacted]`.
- Open the browser devtools network tab on any page and read the
  `Content-Security-Policy` response header.
- Sign in as a student and open `/placement/admin/database`. A 403 page, not a
  redirect and not a partial render.
