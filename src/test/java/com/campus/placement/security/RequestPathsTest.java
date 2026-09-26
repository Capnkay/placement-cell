package com.campus.placement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The officer check must see the same path the container routes on. A doubled
 * slash reaches the same servlet, so it must reach the same rule.
 */
class RequestPathsTest {

    private static final String CTX = "/placement";

    @Test
    @DisplayName("an ordinary admin path is recognised as admin")
    void plainAdminPath() {
        assertTrue(RequestPaths.isAdminPath(RequestPaths.canonical(CTX + "/admin/database", CTX)));
    }

    @Test
    @DisplayName("a doubled slash still counts as an admin path")
    void doubledSlash() {
        assertEquals("/admin/database", RequestPaths.canonical(CTX + "//admin/database", CTX));
        assertTrue(RequestPaths.isAdminPath(RequestPaths.canonical(CTX + "//admin/database", CTX)));
    }

    @Test
    @DisplayName("slashes anywhere in the path are collapsed")
    void manySlashes() {
        assertEquals("/admin/students", RequestPaths.canonical(CTX + "///admin//students", CTX));
    }

    @Test
    @DisplayName("a path parameter cannot hide the prefix")
    void pathParameter() {
        assertEquals("/admin/reports", RequestPaths.canonical(CTX + "/admin/reports;x=1", CTX));
    }

    @Test
    @DisplayName("student pages are not admin pages")
    void studentPath() {
        assertFalse(RequestPaths.isAdminPath(RequestPaths.canonical(CTX + "/app/drives", CTX)));
        assertFalse(RequestPaths.isAdminPath(RequestPaths.canonical(CTX + "/administrator", CTX)));
    }
}
