package com.campus.placement.ejb;

import com.campus.placement.entity.Drive;
import com.campus.placement.entity.DriveStatus;
import com.campus.placement.entity.StudentProfile;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * The eligibility policy, as pure logic.
 *
 * <p>Kept separate from {@link DriveService} on purpose. The rules are the part
 * most worth being certain about, and a static method that takes a student, a
 * drive and one boolean can be tested directly with no database, no container
 * and no mocking. The session bean is left to do what only it can do: fetch the
 * rows and report whether an application already exists.</p>
 */
public final class EligibilityRules {

    private EligibilityRules() {
    }

    /**
     * @param student        the candidate, may be null
     * @param drive          the drive being applied to, may be null
     * @param alreadyApplied whether an application already exists, which the
     *                       caller determines because it needs the database
     * @param today          the date to judge the closing date against, passed in
     *                       rather than read from the clock so a test can pin it
     */
    public static Eligibility check(StudentProfile student, Drive drive,
                                    boolean alreadyApplied, LocalDate today) {
        Eligibility result = new Eligibility();

        if (student == null || drive == null) {
            result.reject("Drive or student record is missing.");
            return result;
        }

        if (drive.getStatus() != DriveStatus.OPEN) {
            result.reject("This drive has been closed by the placement cell.");
        }
        if (drive.getLastDate() != null && drive.getLastDate().isBefore(today)) {
            result.reject("The last date to apply was " + drive.getLastDate() + ".");
        }
        if (student.getCgpa() < drive.getMinCgpa()) {
            result.reject(String.format(Locale.ROOT,
                    "Requires a CGPA of %.2f, your record shows %.2f.",
                    drive.getMinCgpa(), student.getCgpa()));
        }
        if (student.getBacklogs() > drive.getMaxBacklogs()) {
            result.reject("Allows at most " + drive.getMaxBacklogs()
                    + " backlog(s), your record shows " + student.getBacklogs() + ".");
        }

        List<String> branches = drive.getBranchList();
        if (!branches.isEmpty() && branches.stream()
                .noneMatch(b -> b.equalsIgnoreCase(student.getBranch()))) {
            result.reject("Open to " + String.join(", ", branches)
                    + ", your branch is " + student.getBranch() + ".");
        }

        if (alreadyApplied) {
            result.markAlreadyApplied();
        }
        return result;
    }
}
