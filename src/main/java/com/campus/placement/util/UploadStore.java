package com.campus.placement.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

/**
 * Where uploaded resumes live on disk.
 *
 * <p>Files are written outside the deployed application so a redeploy cannot
 * wipe them, and the stored name is generated rather than taken from the
 * browser. That second point matters: a client controlled name like
 * {@code ../../config/domain.xml} is a directory traversal, and the only safe
 * answer is to never use the supplied name as a path at all.</p>
 */
public final class UploadStore {

    private static final String FOLDER = "placement-resumes";

    private UploadStore() {
    }

    public static Path directory() throws IOException {
        String instanceRoot = System.getProperty("com.sun.aas.instanceRoot");
        Path base = instanceRoot != null
                ? Paths.get(instanceRoot, FOLDER)
                : Paths.get(System.getProperty("java.io.tmpdir"), FOLDER);
        Files.createDirectories(base);
        return base;
    }

    /** Builds the on disk name. Only the extension survives from the upload. */
    public static String newFileName(Long profileId, String submittedName) {
        return "resume-" + profileId + "-" + UUID.randomUUID().toString().substring(0, 8)
                + extensionOf(submittedName);
    }

    public static String extensionOf(String submittedName) {
        if (submittedName == null) {
            return "";
        }
        String cleaned = submittedName.trim().toLowerCase(Locale.ROOT);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return "";
        }
        String ext = cleaned.substring(dot);
        return ext.matches("\\.[a-z0-9]{1,5}") ? ext : "";
    }

    /**
     * Resolves a stored name back to a path, refusing anything that tries to
     * climb out of the upload folder.
     */
    public static Path resolve(String storedName) throws IOException {
        if (storedName == null || storedName.isBlank()
                || storedName.contains("/") || storedName.contains("\\")
                || storedName.contains("..")) {
            throw new IOException("Illegal stored file name");
        }
        Path dir = directory();
        Path target = dir.resolve(storedName).normalize();
        if (!target.startsWith(dir)) {
            throw new IOException("Resolved path escaped the upload folder");
        }
        return target;
    }

    public static boolean isAllowedResume(String submittedName) {
        String ext = extensionOf(submittedName);
        return ext.equals(".pdf") || ext.equals(".doc") || ext.equals(".docx");
    }

    /** How many bytes {@link #contentMatches} needs from the front of a file. */
    public static final int SNIFF_BYTES = 8;

    /**
     * Checks that the file actually is what its name claims.
     *
     * <p>An extension is just the end of a string and anybody can type one, so
     * on its own it decides nothing. Every allowed format starts with a known
     * signature, and comparing those first bytes is what makes a script renamed
     * to {@code resume.pdf} fail here rather than sit on the disk waiting to be
     * served back to somebody.</p>
     *
     * @param header    the first {@link #SNIFF_BYTES} bytes of the upload
     * @param extension the extension already checked by {@link #isAllowedResume}
     */
    public static boolean contentMatches(byte[] header, String extension) {
        if (header == null || extension == null) {
            return false;
        }
        return switch (extension) {
            // "%PDF"
            case ".pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46});
            // A .docx is a zip container: "PK\x03\x04"
            case ".docx" -> startsWith(header, new byte[]{0x50, 0x4B, 0x03, 0x04});
            // A .doc is an OLE compound document
            case ".doc" -> startsWith(header, new byte[]{
                    (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
            default -> false;
        };
    }

    private static boolean startsWith(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public static String contentTypeFor(String storedName) {
        String ext = extensionOf(storedName);
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    public static void deleteQuietly(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException ignored) {
            // An orphaned file is not worth failing a request over.
        }
    }
}
