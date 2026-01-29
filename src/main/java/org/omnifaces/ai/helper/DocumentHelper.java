/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.ai.helper;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for document operations.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public final class DocumentHelper {

    private enum MediaType {
        PDF("application/pdf", "pdf"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
        ZIP("application/zip", "zip"),
        CSV("text/csv", "csv"),
        JSON("application/json", "json"),
        HTML("text/html", "html"),
        XML("application/xml", "xml"),
        MARKDOWN("text/markdown", "md"),
        TEXT("text/plain", "txt"),
        BINARY("application/octet-stream", "bin");

        private final String value;
        private final String extension;

        MediaType(String value, String extension) {
            this.value = value;
            this.extension = extension;
        }
    }

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04}; // PK..

    private DocumentHelper() {
        throw new AssertionError();
    }

    /**
     * Guesses the media type of a document based on its magic bytes and content.
     * <p>
     * Supported formats: PDF, DOCX, XLSX, PPTX, CSV, JSON, HTML, XML, Markdown, and plain text.
     *
     * @param content The content bytes to check.
     * @return The guessed media type, or {@code text/plain} for text content, or {@code application/octet-stream} for unknown binary content.
     */
    public static String guessMediaType(byte[] content) {
        if (content == null || content.length == 0) {
            return MediaType.BINARY.value;
        }

        if (startsWith(content, 0, PDF_MAGIC)) {
            return MediaType.PDF.value;
        }

        if (startsWith(content, 0, ZIP_MAGIC)) {
            return guessZipMediaType(content);
        }

        if (isLikelyText(content)) {
            return guessTextMediaType(content);
        }

        return MediaType.BINARY.value;
    }

    /**
     * Returns a file extension for the given media type.
     * <p>
     * The returned extension does not include a leading dot.
     * If the media type is {@code null} or not recognized, {@code "bin"} is returned.
     *
     * @param mediaType The media type (MIME type), e.g. {@code "application/pdf"}.
     * @return The corresponding file extension (without dot), e.g. {@code "pdf"}, or {@code "bin"} if unknown.
     */
    public static String toExtension(String mediaType) {
        return stream(MediaType.values())
                .filter(type -> type.value.equalsIgnoreCase(mediaType))
                .findFirst()
                .map(type -> type.extension)
                .orElse(MediaType.BINARY.extension);
    }

    /**
     * Converts the given content to a Base64 string.
     *
     * @param content The content to be encoded.
     * @return The Base64 encoded string.
     */
    public static String encodeBase64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }

    /**
     * Converts the given content to a data URI.
     *
     * @param content The content bytes.
     * @return The data URI string in the format {@code data:<media-type>;base64,<data>}.
     */
    public static String toDataUri(byte[] content) {
        return toDataUri(guessMediaType(content), content);
    }

    /**
     * Converts the given content to a data URI with the specified media type.
     *
     * @param mediaType The media type (MIME type) for the data URI.
     * @param content The content bytes.
     * @return The data URI string in the format {@code data:<media-type>;base64,<data>}.
     */
    static String toDataUri(String mediaType, byte[] content) {
        return "data:" + mediaType + ";base64," + encodeBase64(content);
    }

    /**
     * Checks if the byte array starts with the given prefix at the specified offset.
     *
     * @param content The byte array to check.
     * @param offset The offset within the content to start checking.
     * @param prefix The prefix bytes to match.
     * @return {@code true} if content contains prefix at the given offset, {@code false} otherwise.
     */
    static boolean startsWith(byte[] content, int offset, byte[] prefix) {
        if (content.length < offset + prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (content[offset + i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Guesses the media type for ZIP-based formats (docx, xlsx, pptx).
     * We peek inside the ZIP to find characteristic files.
     * Falls back to {@code application/zip}.
     */
    private static String guessZipMediaType(byte[] content) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                var name = entry.getName();

                if (name.startsWith("word/")) {
                    return MediaType.DOCX.value;
                }

                if (name.startsWith("xl/")) {
                    return MediaType.XLSX.value;
                }

                if (name.startsWith("ppt/")) {
                    return MediaType.PPTX.value;
                }
            }

            return MediaType.ZIP.value;
        }
        catch (Exception ignore) {
            // Not a valid ZIP or error reading - fall through.
        }

        return MediaType.BINARY.value;
    }

    /**
     * Checks if content is likely text (not binary).
     * Validates UTF-8 encoding and rejects control characters.
     */
    private static boolean isLikelyText(byte[] content) {
        var decoder = UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT);

        try {
            var text = decoder.decode(ByteBuffer.wrap(content, 0, Math.min(content.length, 1024))).toString();

            for (int i = 0; i < text.length(); i++) {
                int codePoint = text.codePointAt(i);

                if (Character.isSupplementaryCodePoint(codePoint)) {
                    i++; // Skip low surrogate of surrogate pair.
                }

                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false; // Reject control characters except whitespace.
                }
            }

            return true;
        }
        catch (CharacterCodingException ignore) {
            return false; // Invalid UTF-8 - likely binary.
        }
    }

    /**
     * Guesses the media type for text-based content.
     * Falls back to {@code text/plain}.
     */
    private static String guessTextMediaType(byte[] content) {
        var text = new String(content, UTF_8).strip();

        if (looksLikeJson(text)) {
            return MediaType.JSON.value;
        }

        if (looksLikeXml(text)) {
            if (looksLikeHtml(text)) {
                return MediaType.HTML.value;
            }

            return MediaType.XML.value;
        }

        if (looksLikeCsv(text)) {
            return MediaType.CSV.value;
        }

        if (looksLikeMarkdown(text)) {
            return MediaType.MARKDOWN.value;
        }

        return MediaType.TEXT.value;
    }

    /**
     * Check if starts with { or [ and ends with } or ].
     */
    private static boolean looksLikeJson(String text) {
        return (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"));
    }

    /**
     * Check if starts with < and ends with >.
     */
    private static boolean looksLikeXml(String text) {
        return text.startsWith("<") && text.endsWith(">");
    }

    /**
     * Check if xml contains doctype or html/head/body tags.
     */
    private static boolean looksLikeHtml(String xml) {
        var lower = xml.substring(0, Math.min(xml.length(), 1024)).toLowerCase();
        return lower.contains("<!doctype html") || lower.contains("<html") || lower.contains("<head") || lower.contains("<body");
    }

    /**
     * Check for consistent comma/semicolon separated values.
     */
    private static boolean looksLikeCsv(String text) {
        var lines = text.split("\n", 10);

        if (lines.length < 2) {
            return false;
        }

        char delimiter = lines[0].contains(";") ? ';' : ',';
        int expectedCount = countChar(lines[0], delimiter);

        if (expectedCount == 0) {
            return false;
        }

        int consistentLines = 0;

        for (var line : lines) {
            if (!line.isBlank() && countChar(line, delimiter) == expectedCount) {
                consistentLines++;
            }
        }

        return consistentLines >= Math.min(lines.length, 3);
    }

    private static int countChar(String s, char c) {
        int count = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }

        return count;
    }

    /**
     * Check for common markdown patterns: headers, links, code blocks.
     */
    private static boolean looksLikeMarkdown(String text) {
        return text.startsWith("# ") || text.startsWith("## ") || text.contains("\n# ") || text.contains("\n## ") || text.contains("](") || text.contains("```");
    }
}
