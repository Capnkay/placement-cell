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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Singleton session bean that fills an empty database on the first start so the
 * portal is usable the moment it deploys. It does nothing at all if accounts
 * already exist, which means restarting the server never wipes real work.
 *
 * <p>Every date is worked out from today, so a freshly seeded database always has
 * drives that are open right now, one closing within days, and older drives that
 * have already finished. To refresh a database whose dates have gone stale, run
 * {@code run.ps1 -Reseed}, which empties the tables before the deploy.</p>
 *
 * <p>Nothing here is random. The applications are derived from the students and
 * drives with plain arithmetic, so two runs give identical data and a demo can be
 * rehearsed.</p>
 */
@Singleton
@Startup
@DependsOn("PortalStatsBean")
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class.getName());

    /** The one password every seeded demo account shares. */
    public static final String DEMO_PASSWORD = "Campus@2026";

    private static final String CS = "Computer Science";
    private static final String IT = "Information Technology";
    private static final String EC = "Electronics";
    private static final String ST = "Statistics";
    private static final String ME = "Mechanical";
    private static final String CV = "Civil";

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
        seedOfficers();
        List<Company> companies = seedCompanies();
        List<Drive> drives = seedDrives(companies);
        List<StudentProfile> students = seedStudents();
        int applications = seedApplications(students, drives);
        LOG.info("Seeded " + students.size() + " students, " + companies.size() + " companies, "
                + drives.size() + " drives and " + applications + " applications. "
                + "Sign in with tpo@campus.edu and the password " + DEMO_PASSWORD);
    }

    private void seedOfficers() {
        officer("tpo@campus.edu", "Meera Raghavan");
        officer("director@campus.edu", "Anil Kulkarni");
        officer("training@campus.edu", "Farah Sheikh");
    }

    private void officer(String email, String name) {
        em.persist(new AppUser(email, PasswordHasher.hash(DEMO_PASSWORD), name, Role.ADMIN));
    }

    private List<Company> seedCompanies() {
        List<Company> companies = List.of(
                company("Northwind Systems", "Product Engineering", "campus@northwind.example"),
                company("Blue Harbour Analytics", "Data and Analytics", "hiring@blueharbour.example"),
                company("Vertex Financial", "Banking Technology", "graduates@vertexfin.example"),
                company("Lumen Health", "Health Informatics", "talent@lumenhealth.example"),
                company("Kestrel Logistics", "Supply Chain Software", "campus@kestrel.example"),
                company("Orbit Semiconductors", "Chip Design", "university@orbitsemi.example"),
                company("Meridian Consulting", "Management Consulting", "recruit@meridian.example"),
                company("Paperkite Learning", "Education Technology", "people@paperkite.example"),
                company("Ironbridge Infrastructure", "Civil and Industrial Projects",
                        "graduates@ironbridge.example"),
                company("Saffron Retail Tech", "Retail Analytics", "hello@saffronretail.example"));
        companies.forEach(em::persist);
        return companies;
    }

    private Company company(String name, String sector, String email) {
        String site = "https://" + email.substring(email.indexOf('@') + 1);
        return new Company(name, sector, email, site);
    }

    private List<Drive> seedDrives(List<Company> c) {
        LocalDate t = LocalDate.now();
        String all = String.join(",", CS, IT, EC, ST, ME, CV);

        List<Drive> drives = List.of(
                // Open now. The last one closes in three days, so the board has urgency.
                drive(c.get(0), "Software Engineer", "Bengaluru", 12.5, 7.0, 0, CS + "," + IT,
                        t.plusDays(21), t.plusDays(10), DriveStatus.OPEN,
                        "Two coding rounds followed by a system design discussion and an HR round. "
                                + "Candidates should be comfortable with data structures and one backend language."),
                drive(c.get(1), "Data Analyst", "Pune", 9.0, 6.5, 1, CS + "," + IT + "," + ST,
                        t.plusDays(28), t.plusDays(16), DriveStatus.OPEN,
                        "Aptitude test, a SQL and spreadsheet round, then a case study presentation."),
                drive(c.get(2), "Technology Analyst", "Mumbai", 8.0, 6.0, 2, CS + "," + IT + "," + EC,
                        t.plusDays(35), t.plusDays(20), DriveStatus.OPEN,
                        "Written test on programming fundamentals, a technical interview and a values interview."),
                drive(c.get(3), "Product Intern", "Remote", 6.0, 7.5, 0, CS + "," + IT,
                        t.plusDays(14), t.plusDays(5), DriveStatus.OPEN,
                        "A six month internship with a pre placement offer for the strongest interns."),
                drive(c.get(4), "Operations Analyst", "Pune", 7.0, 6.0, 2, IT + "," + ME + "," + CV,
                        t.plusDays(25), t.plusDays(12), DriveStatus.OPEN,
                        "Case based aptitude round, a spreadsheet exercise and a panel interview."),
                drive(c.get(5), "Embedded Engineer", "Hyderabad", 10.5, 7.0, 0, EC + "," + CS,
                        t.plusDays(30), t.plusDays(15), DriveStatus.OPEN,
                        "Digital electronics and C programming test, then two technical interviews on firmware."),
                drive(c.get(6), "Associate Consultant", "Gurugram", 8.5, 6.5, 1, all,
                        t.plusDays(40), t.plusDays(24), DriveStatus.OPEN,
                        "Open to every branch. Group discussion, a guesstimate round and a partner interview."),
                drive(c.get(7), "Backend Developer", "Remote", 11.0, 7.5, 0, CS + "," + IT,
                        t.plusDays(18), t.plusDays(8), DriveStatus.OPEN,
                        "Take home API exercise, a live code review and a conversation about past projects."),
                drive(c.get(8), "Graduate Engineer Trainee", "Nagpur", 5.5, 6.0, 3, ME + "," + CV,
                        t.plusDays(33), t.plusDays(18), DriveStatus.OPEN,
                        "Technical written test, site safety module and an interview with the project head."),
                drive(c.get(9), "Data Science Intern", "Bengaluru", 5.0, 8.0, 0, CS + "," + IT + "," + ST,
                        t.plusDays(12), t.plusDays(3), DriveStatus.OPEN,
                        "Statistics and Python assessment, then a modelling case on real retail data."),
                // Finished, kept on the board so the archive and the results have history.
                drive(c.get(0), "QA Engineer", "Hyderabad", 7.5, 6.0, 1, CS + "," + IT + "," + EC,
                        t.minusDays(20), t.minusDays(30), DriveStatus.CLOSED,
                        "Closed drive kept on the board so the archive view has something to show."),
                drive(c.get(2), "Risk Analyst", "Mumbai", 9.5, 7.0, 0, CS + "," + IT + "," + ST,
                        t.minusDays(25), t.minusDays(40), DriveStatus.CLOSED,
                        "Quantitative test, a probability interview and a final round with the risk head."),
                drive(c.get(5), "Test Engineer", "Bengaluru", 7.0, 6.0, 1, EC,
                        t.minusDays(15), t.minusDays(28), DriveStatus.CLOSED,
                        "Lab based practical test and a technical interview."),
                drive(c.get(6), "Business Analyst", "Pune", 7.0, 6.5, 1, all,
                        t.minusDays(35), t.minusDays(50), DriveStatus.CLOSED,
                        "Aptitude, a case study and a managerial interview. Offers have been released."));
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

    /**
     * The first six are the accounts the demo signs in with. The rest are spread
     * across every branch and every CGPA band on purpose: a few are placed, some
     * are ineligible for most drives, three have not filled in a phone number, and
     * one account is suspended, so each admin screen has something real to show.
     */
    private List<StudentProfile> seedStudents() {
        return List.of(
                student("aarti.deshpande@campus.edu", "Aarti Deshpande", "CS21001", CS, 2026, 8.7, 0, "9820011223"),
                student("rahul.menon@campus.edu", "Rahul Menon", "CS21014", CS, 2026, 7.2, 1, "9820044556"),
                student("sneha.pillai@campus.edu", "Sneha Pillai", "IT21007", IT, 2026, 9.1, 0, "9820077889"),
                student("imran.qureshi@campus.edu", "Imran Qureshi", "IT21022", IT, 2026, 6.4, 2, "9820099001"),
                student("tanvi.joshi@campus.edu", "Tanvi Joshi", "EC21005", EC, 2026, 8.0, 0, "9820022334"),
                student("dev.narang@campus.edu", "Dev Narang", "CS21030", CS, 2027, 7.8, 0, "9820055667"),
                student("priya.nair@campus.edu", "Priya Nair", "CS21041", CS, 2026, 9.3, 0, "9820100101"),
                student("arjun.bhatt@campus.edu", "Arjun Bhatt", "CS21052", CS, 2026, 6.8, 0, "9820100202"),
                student("kavya.iyer@campus.edu", "Kavya Iyer", "CS21063", CS, 2026, 8.2, 1, "9820100303"),
                student("rohan.kapoor@campus.edu", "Rohan Kapoor", "CS21074", CS, 2026, 5.6, 3, null),
                student("ishita.rao@campus.edu", "Ishita Rao", "CS21085", CS, 2026, 7.5, 0, "9820100505"),
                student("manav.shah@campus.edu", "Manav Shah", "CS21096", CS, 2027, 8.9, 0, "9820100606"),
                student("neha.kulkarni@campus.edu", "Neha Kulkarni", "IT21031", IT, 2026, 7.9, 0, "9820100707"),
                student("farhan.ansari@campus.edu", "Farhan Ansari", "IT21042", IT, 2026, 6.9, 1, "9820100808"),
                student("divya.menon@campus.edu", "Divya Menon", "IT21053", IT, 2026, 8.4, 0, null),
                student("yash.thakur@campus.edu", "Yash Thakur", "IT21064", IT, 2026, 5.9, 2, "9820101010"),
                student("meghna.das@campus.edu", "Meghna Das", "EC21016", EC, 2026, 7.7, 0, "9820101111"),
                student("siddharth.jain@campus.edu", "Siddharth Jain", "EC21027", EC, 2026, 6.2, 1, "9820101212"),
                student("pooja.verma@campus.edu", "Pooja Verma", "EC21038", EC, 2026, 8.6, 0, "9820101313"),
                student("aditya.rane@campus.edu", "Aditya Rane", "ST21009", ST, 2026, 8.1, 0, "9820101414"),
                student("zoya.khan@campus.edu", "Zoya Khan", "ST21018", ST, 2026, 7.0, 1, null),
                student("vikram.patil@campus.edu", "Vikram Patil", "ME21011", ME, 2026, 6.6, 2, "9820101616"),
                student("anjali.gupta@campus.edu", "Anjali Gupta", "ME21022", ME, 2026, 7.4, 0, "9820101717"),
                suspended(student("nikhil.sawant@campus.edu", "Nikhil Sawant", "CV21008", CV, 2026, 6.1, 1,
                        "9820101818")),
                student("ritika.sharma@campus.edu", "Ritika Sharma", "CV21019", CV, 2026, 7.3, 0, "9820101919"));
    }

    private StudentProfile suspended(StudentProfile profile) {
        profile.getUser().setActive(false);
        return profile;
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

    /**
     * Six hand picked applications keep the story of the demo accounts, then every
     * other student is matched against every drive. A pair only becomes an
     * application when the student really satisfies the drive's rules, which is the
     * same test the portal applies, so nothing here is something the app would
     * refuse. Each student ends up with at most one accepted offer.
     */
    private int seedApplications(List<StudentProfile> students, List<Drive> drives) {
        Set<String> made = new HashSet<>();
        Set<Integer> placed = new HashSet<>();
        int count = 0;

        count += apply(made, students, drives, 0, 0, ApplicationStatus.SHORTLISTED,
                "Cleared the online round with a strong score");
        count += apply(made, students, drives, 0, 1, ApplicationStatus.APPLIED, null);
        count += apply(made, students, drives, 2, 0, ApplicationStatus.INTERVIEW,
                "Design round scheduled for next week");
        count += apply(made, students, drives, 2, 1, ApplicationStatus.APPLIED, null);
        count += apply(made, students, drives, 1, 2, ApplicationStatus.SELECTED,
                "Offer released, joining in July");
        placed.add(1);
        count += apply(made, students, drives, 4, 2, ApplicationStatus.REJECTED,
                "Did not clear the written round");
        // Aarti and Sneha are the accounts the demo signs in with. They are kept
        // free of offers so there is always a live application to make and follow.
        placed.add(0);
        placed.add(2);

        for (int s = 0; s < students.size(); s++) {
            for (int d = 0; d < drives.size(); d++) {
                if (made.contains(s + ":" + d) || !meets(students.get(s), drives.get(d))) {
                    continue;
                }
                int key = (s * 31 + d * 17) % 10;
                if (key >= 5) {
                    continue;
                }
                boolean closed = drives.get(d).getStatus() == DriveStatus.CLOSED;
                ApplicationStatus status;
                String note;
                if (closed) {
                    boolean offer = key == 0 && !placed.contains(s);
                    status = offer ? ApplicationStatus.SELECTED : ApplicationStatus.REJECTED;
                    note = offer ? "Offer accepted, joining after graduation" : "Not taken forward after the final round";
                } else if ((s + d) % 13 == 0 && !placed.contains(s)) {
                    status = ApplicationStatus.SELECTED;
                    note = "Offer released, awaiting acceptance";
                } else {
                    status = switch (key) {
                        case 0 -> ApplicationStatus.SHORTLISTED;
                        case 2 -> ApplicationStatus.INTERVIEW;
                        case 4 -> ApplicationStatus.REJECTED;
                        default -> ApplicationStatus.APPLIED;
                    };
                    note = switch (status) {
                        case SHORTLISTED -> "Shortlisted after the screening round";
                        case INTERVIEW -> "Interview slot will be emailed";
                        case REJECTED -> "Profile did not match the current requirement";
                        default -> null;
                    };
                }
                if (status == ApplicationStatus.SELECTED) {
                    placed.add(s);
                }
                count += apply(made, students, drives, s, d, status, note);
            }
        }
        return count;
    }

    /** The same three rules {@code EligibilityRules} applies to a live application. */
    private boolean meets(StudentProfile student, Drive drive) {
        return drive.getBranchList().contains(student.getBranch())
                && student.getCgpa() >= drive.getMinCgpa()
                && student.getBacklogs() <= drive.getMaxBacklogs();
    }

    private int apply(Set<String> made, List<StudentProfile> students, List<Drive> drives,
                      int s, int d, ApplicationStatus status, String remarks) {
        if (!made.add(s + ":" + d)) {
            return 0;
        }
        boolean closed = drives.get(d).getStatus() == DriveStatus.CLOSED;
        JobApplication application = new JobApplication(students.get(s), drives.get(d));
        application.setStatus(status);
        application.setRemarks(remarks);
        application.setAppliedAt(LocalDateTime.now()
                .minusDays((closed ? 55 : 1) + (s * 3 + d * 5) % 25)
                .minusHours((s + d) % 9));
        em.persist(application);
        return 1;
    }
}
