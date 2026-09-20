package com.campus.placement.ejb;

import com.campus.placement.entity.AppUser;
import java.io.Serializable;

/**
 * Outcome of a sign in attempt. The message is written for the person at the
 * keyboard, so it never says whether the email exists: a wrong address and a
 * wrong password give the same sentence, which stops the login form being used
 * to discover valid accounts.
 */
public class AuthResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Outcome {
        SUCCESS,
        INVALID_INPUT,
        BAD_CREDENTIALS,
        LOCKED,
        DISABLED
    }

    private final Outcome outcome;
    private final AppUser user;
    private final String message;
    private final int attemptsLeft;

    private AuthResult(Outcome outcome, AppUser user, String message, int attemptsLeft) {
        this.outcome = outcome;
        this.user = user;
        this.message = message;
        this.attemptsLeft = attemptsLeft;
    }

    public static AuthResult success(AppUser user) {
        return new AuthResult(Outcome.SUCCESS, user, "Signed in", 0);
    }

    public static AuthResult invalidInput(String message) {
        return new AuthResult(Outcome.INVALID_INPUT, null, message, 0);
    }

    public static AuthResult badCredentials(int attemptsLeft) {
        String message = attemptsLeft > 0 && attemptsLeft <= 2
                ? "Email or password is incorrect. " + attemptsLeft
                        + " attempt" + (attemptsLeft == 1 ? "" : "s") + " left before this account is locked."
                : "Email or password is incorrect.";
        return new AuthResult(Outcome.BAD_CREDENTIALS, null, message, attemptsLeft);
    }

    public static AuthResult locked(long minutes) {
        return new AuthResult(Outcome.LOCKED, null,
                "Too many failed attempts. This account is locked for another "
                        + Math.max(1, minutes) + " minute" + (minutes == 1 ? "" : "s") + ".", 0);
    }

    public static AuthResult disabled() {
        return new AuthResult(Outcome.DISABLED, null,
                "This account has been deactivated. Contact the placement cell.", 0);
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public AppUser getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }
}
