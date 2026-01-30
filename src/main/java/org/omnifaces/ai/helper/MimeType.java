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

import java.util.Base64;

/**
 * Represents a MIME type with its associated file extension.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public interface MimeType {

    /**
     * Returns the MIME type string.
     *
     * @return The MIME type string (e.g., "application/pdf").
     */
    String value();

    /**
     * Returns the file extension.
     *
     * @return The file extension without a leading dot (e.g., "pdf").
     */
    String extension();

    /**
     * Converts the given content to a Base64 string.
     *
     * @param content The content to be encoded.
     * @return The Base64 encoded string.
     */
    default String toBase64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }

    /**
     * Converts the given content to a data URI.
     *
     * @param content The content bytes.
     * @return The data URI string in the format {@code data:<mime-type>;base64,<data>}.
     */
    default String toDataUri(byte[] content) {
        return "data:" + value() + ";base64," + toBase64(content);
    }
}
