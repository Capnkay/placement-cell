# Work split across the five members

Divided by layer, so each member owns whole files. Nobody has to explain code
somebody else wrote, and the boundaries follow the ones the course itself draws.
Each person has a brief in `docs/team/` with their files, a script, a VS Code
walk, a live demo and the questions they should expect. Start with
`docs/team/00-start-here.html`.

| Member | Owns | Brief |
|---|---|---|
| **Karan** | The app as a whole: users, data in and out, the student and officer flows, the dashboards, the seed data, the demo | `01-karan-app-overview` |
| **Yuvraj** | MySQL connectivity: the JDBC layer, reports, the database page, the connection pool, the classroom style JDBC bean | `02-yuvraj-mysql-jdbc` |
| **Tiya** | Accounts, sign in, passwords, sessions, filters, cookies, security headers | `03-tiya-auth-security` |
| **Kinjal** | Everything a person sees, types or uploads: JSP views, validation, resume upload, bulk import | `04-kinjal-views-validation` |
| **Chandra** | The domain model and business logic: JPA entities with Hibernate, every EJB, the audit interceptor, the tests | `05-chandra-entities-ejb` |

Shared documents: `06-viva-questions` (every expected question with an owner) and
`07-demo-run-sheet` (the order and timings of the presentation).

## Files by owner

**Karan**

- `ejb/DataSeeder.java`
- `web/StudentDashboardServlet.java`, `DriveBrowseServlet.java`, `ApplicationServlet.java`
- `web/AdminDashboardServlet.java`, `AdminDriveServlet.java`, `AdminCompanyServlet.java`,
  `AdminStudentServlet.java`, `AdminApplicationServlet.java`, `ShortlistServlet.java`,
  `AuditServlet.java`, `SystemServlet.java`
- `run.ps1`, `README.md`, `docs/`

**Yuvraj**

- everything in `jdbc/`
- `web/ReportsServlet.java`, `DatabaseExplorerServlet.java`, `ClassroomJdbcServlet.java`
- `ejb/MockScoreBean.java`
- `WEB-INF/glassfish-resources.xml`
- `views/admin-reports.jsp`, `admin-database.jsp`, `admin-classroom.jsp`
- `test/jdbc/*`

**Tiya**

- `web/LoginServlet.java`, `RegisterServlet.java`, `LogoutServlet.java`, `ThemeServlet.java`,
  `SessionUser.java`, `SessionTracker.java`, `Web.java`
- everything in `security/`
- `ejb/AuthService.java`, `AuthResult.java`
- `util/EmailDomainVerifier.java`
- `WEB-INF/web.xml`
- `views/login.jsp`, `register.jsp`
- `test/security/*`, `test/util/EmailDomainVerifierTest.java`

**Kinjal**

- all other JSPs and fragments in `WEB-INF/views/`, and everything in `webapp/assets/`
- `util/Validators.java`, `Branches.java`, `UploadStore.java`
- `web/ProfileServlet.java`, `ResumeDownloadServlet.java`, `BulkImportServlet.java`
- `test/util/ValidatorsTest.java`, `BranchesTest.java`, `UploadStoreTest.java`

**Chandra**

- everything in `entity/`
- `resources/META-INF/persistence.xml`
- `ejb/ApplicationService.java`, `DriveService.java`, `StudentService.java`, `Eligibility.java`,
  `EligibilityRules.java`, `ShortlistBean.java`, `NotificationMDB.java`, `NotificationSender.java`,
  `PortalStatsBean.java`, `AuditInterceptor.java`, `AuditService.java`, `AuditWriter.java`
- `test/ejb/EligibilityRulesTest.java`

## Rule of the room

If the teacher asks about a file, the owner answers. If the question crosses two layers
(for example "what happens when I press Apply"), the person whose layer the request is in
right now answers first, and passes on with "the next step is Chandra's".
