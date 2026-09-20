package com.campus.placement.ejb;

import com.campus.placement.entity.AppUser;
import com.campus.placement.entity.ApplicationStatus;
import com.campus.placement.entity.Company;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.DriveStatus;
import com.campus.placement.entity.JobApplication;
import com.campus.placement.entity.Role;
import com.campus.placement.entity.StudentProfile;
import com.campus.placement.security.PasswordHasher;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Singleton session bean that fills an empty database on the first start so the
 * portal is usable the moment it deploys. It does nothing at all if accounts
 * already exist, which means restarting the server never wipes real work.
 */
@Singleton
@Startup
@DependsOn("PortalStatsBean")
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class.getName());

    /** The one password every seeded demo account shares. */
    public static final String DEMO_PASSWORD = "Campus@2026";

    @PersistenceContext(unitName = "placementPU")
    private EntityManager em;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void seed() {
        Long existing = em.createQuery("SELECT COUNT(u) FROM AppUser u", Long.class).getSingleResult();
        if (existing > 0) {
            LOG.info("Database already holds " + existing + " accounts, seeding skipped");
            return;
        }

        LOG.info("Empty database found, seeding the placement cell with demo data");
        seedOfficer();
        List<Company> companies = seedCompanies();
        List<Drive> drives = seedDrives(companies);
        List<StudentProfile> students = seedStudents();
        seedApplications(students, drives);
        LOG.info("Seeding finished. Sign in with tpo@campus.edu and the password " + DEMO_PASSWORD);
    }

    private void seedOfficer() {
        AppUser officer = new AppUser("tpo@campus.edu", PasswordHasher.hash(DEMO_PASSWORD),
                "Meera Raghavan", Role.ADMIN);
        em.persist(officer);

        AppUser second = new AppUser("director@campus.edu", PasswordHasher.hash(DEMO_PASSWORD),
                "Anil Kulkarni", Role.ADMIN);
        em.persist(second);
    }

    private List<Company> seedCompanies() {
        List<Company> companies = List.of(
                new Company("Northwind Systems", "Product Engineering",
                        "campus@northwind.example", "https://northwind.example"),
                new Company("Blue Harbour Analytics", "Data and Analytics",
                        "hiring@blueharbour.example", "https://blueharbour.example"),
                new Company("Vertex Financial", "Banking Technology",
                        "graduates@vertexfin.example", "https://vertexfin.example"),
                new Company("Lumen Health", "Health Informatics",
                        "talent@lumenhealth.example", "https://lumenhealth.example"));
        companies.forEach(em::persist);
        return companies;
    }

    private List<Drive> seedDrives(List<Company> companies) {
        LocalDate today = LocalDate.now();

        Drive sde = drive(companies.get(0), "Software Engineer", "Bengaluru", 12.5,
                7.0, 0, "Computer Science,Information Technology",
                today.plusDays(21), today.plusDays(10), DriveStatus.OPEN,
                "Two coding rounds followed by a system design discussion and an HR round. "
                        + "Candidates should be comfortable with data structures and one backend language.");

        Drive analyst = drive(companies.get(1), "Data Analyst", "Pune", 9.0,
                6.5, 1, "Computer Science,Information Technology,Statistics",
                today.plusDays(28), today.plusDays(16), DriveStatus.OPEN,
                "Aptitude test, a SQL and spreadsheet round, then a case study presentation.");

        Drive support = drive(companies.get(2), "Technology Analyst", "Mumbai", 8.0,
                6.0, 2, "Computer Science,Information Technology,Electronics",
                today.plusDays(35), today.plusDays(20), DriveStatus.OPEN,
                "Written test on programming fundamentals, a technical interview and a values interview.");

        Drive intern = drive(companies.get(3), "Product Intern", "Remote", 6.0,
                7.5, 0, "Computer Science,Information Technology",
                today.plusDays(14), today.plusDays(5), DriveStatus.OPEN,
                "A six month internship with a pre placement offer for the strongest interns.");

        Drive closed = drive(companies.get(0), "QA Engineer", "Hyderabad", 7.5,
                6.0, 1, "Computer Science,Information Technology,Electronics",
                today.minusDays(20), today.minusDays(30), DriveStatus.CLOSED,
                "Closed drive kept on the board so the archive view has something to show.");

        List<Drive> drives = List.of(sde, analyst, support, intern, closed);
        drives.forEach(em::persist);
        return drives;
    }

    private Drive drive(Company company, String role, String location, double pkg,
                        double minCgpa, int maxBacklogs, String branches,
                        LocalDate driveDate, LocalDate lastDate, DriveStatus status,
                        String description) {
        Drive drive = new Drive();
        drive.setCompany(company);
        drive.setJobRole(role);
        drive.setLocation(location);
        drive.setPackageLpa(pkg);
        drive.setMinCgpa(minCgpa);
        drive.setMaxBacklogs(maxBacklogs);
        drive.setAllowedBranches(branches);
        drive.setDriveDate(driveDate);
        drive.setLastDate(lastDate);
        drive.setStatus(status);
        drive.setDescription(description);
        return drive;
    }

    private List<StudentProfile> seedStudents() {
        return List.of(
                student("aarti.deshpande@campus.edu", "Aarti Deshpande", "CS21001",
                        "Computer Science", 2026, 8.7, 0, "9820011223"),
                student("rahul.menon@campus.edu", "Rahul Menon", "CS21014",
                        "Computer Science", 2026, 7.2, 1, "9820044556"),
                student("sneha.pillai@campus.edu", "Sneha Pillai", "IT21007",
                        "Information Technology", 2026, 9.1, 0, "9820077889"),
                student("imran.qureshi@campus.edu", "Imran Qureshi", "IT21022",
                        "Information Technology", 2026, 6.4, 2, "9820099001"),
                student("tanvi.joshi@campus.edu", "Tanvi Joshi", "EC21005",
                        "Electronics", 2026, 8.0, 0, "9820022334"),
                student("dev.narang@campus.edu", "Dev Narang", "CS21030",
                        "Computer Science", 2027, 7.8, 0, "9820055667"));
    }

    private StudentProfile student(String email, String name, String rollNo, String branch,
                                   int batch, double cgpa, int backlogs, String phone) {
        AppUser user = new AppUser(email, PasswordHasher.hash(DEMO_PASSWORD), name, Role.STUDENT);
        em.persist(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setRollNo(rollNo);
        profile.setBranch(branch);
        profile.setBatchYear(batch);
        profile.setCgpa(cgpa);
        profile.setBacklogs(backlogs);
        profile.setPhone(phone);
        em.persist(profile);
        return profile;
    }

    private void seedApplications(List<StudentProfile> students, List<Drive> drives) {
        Drive sde = drives.get(0);
        Drive analyst = drives.get(1);
        Drive support = drives.get(2);

        apply(students.get(0), sde, ApplicationStatus.SHORTLISTED,
                "Cleared the online round with a strong score");
        apply(students.get(2), sde, ApplicationStatus.INTERVIEW,
                "Design round scheduled for next week");
        apply(students.get(0), analyst, ApplicationStatus.APPLIED, null);
        apply(students.get(1), support, ApplicationStatus.SELECTED,
                "Offer released, joining in July");
        apply(students.get(4), support, ApplicationStatus.REJECTED,
                "Did not clear the written round");
        apply(students.get(2), analyst, ApplicationStatus.APPLIED, null);
    }

    private void apply(StudentProfile student, Drive drive, ApplicationStatus status, String remarks) {
        JobApplication application = new JobApplication(student, drive);
        application.setStatus(status);
        application.setRemarks(remarks);
        em.persist(application);
    }
}
