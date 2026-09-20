package com.campus.placement.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why a student can or cannot apply to a drive. The reasons are shown next to
 * the Apply button so the student is never left guessing.
 */
public class Eligibility implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> reasons = new ArrayList<>();
    private boolean alreadyApplied;

    public void reject(String reason) {
        reasons.add(reason);
    }

    public void markAlreadyApplied() {
        this.alreadyApplied = true;
    }

    public boolean isEligible() {
        return reasons.isEmpty();
    }

    public boolean isAlreadyApplied() {
        return alreadyApplied;
    }

    public boolean isCanApply() {
        return isEligible() && !alreadyApplied;
    }

    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }

    public String getFirstReason() {
        return reasons.isEmpty() ? "" : reasons.get(0);
    }
}
