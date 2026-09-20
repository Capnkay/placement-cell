package com.campus.placement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Resume upload storage")
class UploadStoreTest {

    @Test
    @DisplayName("only document extensions are accepted")
    void acceptsOnlyDocuments() {
        assertTrue(UploadStore.isAllowedResume("Resume.pdf"));
        assertTrue(UploadStore.isAllowedResume("resume.PDF"));
        assertTrue(UploadStore.isAllowedResume("my cv.docx"));
        assertTrue(UploadStore.isAllowedResume("cv.doc"));

        assertFalse(UploadStore.isAllowedResume("shell.jsp"));
        assertFalse(UploadStore.isAllowedResume("payload.exe"));
        assertFalse(UploadStore.isAllowedResume("archive.zip"));
        assertFalse(UploadStore.isAllowedResume("noextension"));
        assertFalse(UploadStore.isAllowedResume(null));
    }

    @Test
    @DisplayName("a double extension does not slip through")
    void refusesDoubleExtensions() {
        // Only the final extension counts, so this is a .jsp and is refused.
        assertFalse(UploadStore.isAllowedResume("resume.pdf.jsp"));
    }

    @Test
    @DisplayName("the stored name is generated, not taken from the browser")
    void generatesTheStoredName() {
        String stored = UploadStore.newFileName(7L, "My Resume (final).pdf");

        assertTrue(stored.startsWith("resume-7-"));
        assertTrue(stored.endsWith(".pdf"));
        assertFalse(stored.contains(" "), "no spaces in a name that becomes a path");
        assertFalse(stored.contains("("), "nothing from the submitted name survives except the extension");
    }

    @Test
    @DisplayName("two uploads of the same file do not collide")
    void generatesUniqueNames() {
        assertFalse(UploadStore.newFileName(7L, "cv.pdf")
                .equals(UploadStore.newFileName(7L, "cv.pdf")));
    }

    @ParameterizedTest
    @DisplayName("a traversing name is refused instead of resolved")
    @ValueSource(strings = {
            "../../config/domain.xml",
            "..\\..\\config\\domain.xml",
            "sub/dir/resume.pdf",
            "..",
            ""
    })
    void refusesTraversal(String name) {
        assertThrows(IOException.class, () -> UploadStore.resolve(name));
    }

    @Test
    @DisplayName("a null stored name is refused")
    void refusesNull() {
        assertThrows(IOException.class, () -> UploadStore.resolve(null));
    }

    @Test
    @DisplayName("extensions are read from the last dot only")
    void readsTheLastExtension() {
        assertEquals(".pdf", UploadStore.extensionOf("a.b.c.pdf"));
        assertEquals("", UploadStore.extensionOf("trailing."));
        assertEquals("", UploadStore.extensionOf("none"));
        assertEquals("", UploadStore.extensionOf(null));
        assertEquals("", UploadStore.extensionOf("weird.extensionthatistoolong"));
    }

    @Test
    @DisplayName("the content type follows the extension")
    void mapsContentTypes() {
        assertEquals("application/pdf", UploadStore.contentTypeFor("x.pdf"));
        assertEquals("application/msword", UploadStore.contentTypeFor("x.doc"));
        assertEquals("application/octet-stream", UploadStore.contentTypeFor("x.txt"));
    }
}
