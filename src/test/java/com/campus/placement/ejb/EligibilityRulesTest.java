package com.campus.placement.ejb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campus.placement.entity.Company;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.DriveStatus;
import com.campus.placement.entity.StudentProfile;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The eligibility rules, tested without a database or a container.
 *
 * <p>These are the assertions that matter most: an eligibility bug either lets a
 * student apply to something they should not, or hides a drive they were
 * entitled to. Both are invisible until somebody complains.</p>
 */
@DisplayName("Eligibility rules")
class EligibilityRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    private Drive drive(double minCgpa, int maxBacklogs, String branches,
                        DriveStatus status, LocalDate lastDate) {
        Drive drive = new Drive();
        drive.setCompany(new Company("Northwind", "Product", "hr@northwind.example", null));
        drive.setJobRole("Software Engineer");
        drive.setLocation("Bengaluru");
        drive.setPackageLpa(12);
        drive.setMinCgpa(minCgpa);
        drive.setMaxBacklogs(maxBacklogs);
        drive.setAllowedBranches(branches);
        drive.setStatus(status);
        drive.setDriveDate(lastDate.plusDays(5));
        drive.setLastDate(lastDate);
        return drive;
    }

    private StudentProfile student(double cgpa, int backlogs, String branch) {
        StudentProfile profile = new StudentProfile();
        profile.setRollNo("CS21001");
        profile.setBranch(branch);
        profile.setBatchYear(2026);
        profile.setCgpa(cgpa);
        profile.setBacklogs(backlogs);
        return profile;
    }

    @Test
    @DisplayName("a qualifying student can apply")
    void allowsAQualifyingStudent() {
        Eligibility result = EligibilityRules.check(
                student(8.7, 0, "Computer Science"),
                drive(7.0, 0, "Computer Science,Information Technology",
                        DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY);

        assertTrue(result.isEligible());
        assertTrue(result.isCanApply());
        assertFalse(result.isAlreadyApplied());
        assertTrue(result.getReasons().isEmpty());
    }

    @Test
    @DisplayName("the CGPA boundary is inclusive")
    void cgpaBoundaryIsInclusive() {
        Eligibility exactly = EligibilityRules.check(
                student(7.0, 0, "Computer Science"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY);
        assertTrue(exactly.isEligible(), "a student on exactly the minimum qualifies");

        Eligibility justUnder = EligibilityRules.check(
                student(6.99, 0, "Computer Science"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY);
        assertFalse(justUnder.isEligible());
    }

    @Test
    @DisplayName("the backlog limit is inclusive")
    void backlogBoundaryIsInclusive() {
        assertTrue(EligibilityRules.check(
                student(8.0, 1, "Computer Science"),
                drive(7.0, 1, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY).isEligible());

        assertFalse(EligibilityRules.check(
                student(8.0, 2, "Computer Science"),
                drive(7.0, 1, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY).isEligible());
    }

    @Test
    @DisplayName("the closing date is inclusive on the day itself")
    void closingDateIsInclusive() {
        assertTrue(EligibilityRules.check(
                student(8.0, 0, "Computer Science"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY),
                false, TODAY).isEligible(), "applications close at the end of the last date");

        assertFalse(EligibilityRules.check(
                student(8.0, 0, "Computer Science"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY.minusDays(1)),
                false, TODAY).isEligible());
    }

    @Test
    @DisplayName("a closed drive refuses everybody")
    void closedDriveRefusesEveryone() {
        Eligibility result = EligibilityRules.check(
                student(10.0, 0, "Computer Science"),
                drive(0, 99, "Computer Science", DriveStatus.CLOSED, TODAY.plusDays(5)),
                false, TODAY);
        assertFalse(result.isEligible());
        assertTrue(result.getFirstReason().contains("closed"));
    }

    @Test
    @DisplayName("branch matching ignores case but not identity")
    void matchesBranchCaseInsensitively() {
        assertTrue(EligibilityRules.check(
                student(8.0, 0, "computer science"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY).isEligible());

        assertFalse(EligibilityRules.check(
                student(8.0, 0, "Mechanical"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY).isEligible());
    }

    @Test
    @DisplayName("every failing rule is reported, not just the first")
    void collectsEveryReason() {
        Eligibility result = EligibilityRules.check(
                student(5.0, 4, "Mechanical"),
                drive(8.0, 0, "Computer Science", DriveStatus.CLOSED, TODAY.minusDays(2)),
                false, TODAY);

        assertFalse(result.isEligible());
        assertEquals(5, result.getReasons().size(),
                "closed, expired, CGPA, backlogs and branch should all be listed");
    }

    @Test
    @DisplayName("an eligible student who already applied cannot apply again")
    void blocksASecondApplication() {
        Eligibility result = EligibilityRules.check(
                student(8.7, 0, "Computer Science"),
                drive(7.0, 0, "Computer Science", DriveStatus.OPEN, TODAY.plusDays(5)),
                true, TODAY);

        assertTrue(result.isEligible(), "the student still meets every rule");
        assertTrue(result.isAlreadyApplied());
        assertFalse(result.isCanApply(), "but the Apply button must not be offered");
    }

    @Test
    @DisplayName("a drive open to no branch at all accepts any branch")
    void emptyBranchListMeansNoBranchRestriction() {
        assertTrue(EligibilityRules.check(
                student(8.0, 0, "Civil"),
                drive(7.0, 0, "", DriveStatus.OPEN, TODAY.plusDays(5)),
                false, TODAY).isEligible());
    }

    @Test
    @DisplayName("missing records are refused rather than crashing")
    void handlesMissingRecords() {
        assertFalse(EligibilityRules.check(null, null, false, TODAY).isEligible());
        assertFalse(EligibilityRules.check(
                student(8.0, 0, "Computer Science"), null, false, TODAY).isEligible());
    }
}
