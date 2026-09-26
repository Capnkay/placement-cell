-- Queries for the live demo. Run against the placement_cell database
-- (user placement_user, password Placement@2026, localhost:3306).
-- All are read only, so they are safe to run while presenting.

-- 1. Tiya: the account just created. The password column holds a salted hash,
--    never the text that was typed. Compare two rows to see different salts.
SELECT id, email, user_role, LEFT(password_hash, 46) AS hash_start
FROM app_user
WHERE email = 'demo.student.cs@gmail.com' OR email = 'tpo@campus.edu';

-- 2. Tiya: the student's academic record, linked to the account.
SELECT u.email, s.roll_no, s.branch, s.batch_year, s.cgpa, s.backlogs, s.phone
FROM student_profile s
JOIN app_user u ON u.id = s.user_id
WHERE u.email = 'demo.student.cs@gmail.com';

-- 3. Chandra and Yuvraj: the application created during the demo, joined across
--    four tables the way the ORM joins them.
SELECT u.email, c.name AS company, d.job_role, a.status, a.applied_at, a.remarks
FROM job_application a
JOIN student_profile s ON s.id = a.student_id
JOIN app_user u ON u.id = s.user_id
JOIN drive d ON d.id = a.drive_id
JOIN company c ON c.id = d.company_id
WHERE u.email = 'demo.student.cs@gmail.com';

-- 4. Chandra: the audit trail written by the interceptor. MySQL stores times in UTC, so
--    they read 5 hours 30 minutes behind the clock on the pages. Read only lookups are
--    not recorded, and no password ever appears.
SELECT logged_at, actor, action, detail, duration_ms
FROM audit_log
ORDER BY id DESC
LIMIT 10;

-- 5. Yuvraj: the shape of the tables as MySQL holds them.
SHOW TABLES;
DESCRIBE job_application;
SHOW CREATE TABLE job_application;

-- 6. Yuvraj: the unique constraint that stops a second application to one drive.
SELECT index_name, non_unique, column_name
FROM information_schema.statistics
WHERE table_schema = 'placement_cell' AND table_name = 'job_application';

-- 7. Yuvraj: the classroom style page writes here. The table is created by its first
--    classroom save, so this errors with "doesn't exist" until one has been made.
SELECT * FROM mock_score ORDER BY id DESC;

-- 8. Karan: the totals the officer overview shows.
SELECT
  (SELECT COUNT(*) FROM app_user WHERE user_role = 'STUDENT') AS students,
  (SELECT COUNT(*) FROM company) AS companies,
  (SELECT COUNT(*) FROM drive WHERE status = 'OPEN') AS open_drives,
  (SELECT COUNT(*) FROM job_application) AS applications,
  (SELECT COUNT(DISTINCT student_id) FROM job_application WHERE status = 'SELECTED') AS placed;
