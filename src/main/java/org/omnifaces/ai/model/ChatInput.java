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
package org.omnifaces.ai.model;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.omnifaces.ai.helper.DocumentHelper.encodeBase64;
import static org.omnifaces.ai.helper.DocumentHelper.guessMediaType;
import static org.omnifaces.ai.helper.DocumentHelper.toDataUri;
import static org.omnifaces.ai.helper.DocumentHelper.toExtension;
import static org.omnifaces.ai.helper.ImageHelper.isSupportedImage;
import static org.omnifaces.ai.helper.ImageHelper.sanitizeImage;
import static org.omnifaces.ai.helper.ImageHelper.toImageDataUri;
import static org.omnifaces.ai.helper.ImageHelper.toImageMediaType;
import static org.omnifaces.ai.helper.TextHelper.requireNonBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Input for chat-based AI interactions.
 * <p>
 * This class encapsulates the user input for AI chat operations, including the text message and optional file attachments (images and documents).
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public class ChatInput implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Represents an attached image.
     * @param content The image content bytes.
     * @param mediaType The image media type (e.g., "image/png").
     * @param fileName The image file name.
     */
    public final record Image(byte[] content, String mediaType, String fileName) implements Serializable {

        /**
         * Returns the image content as a Base64 encoded string.
         * @return The Base64 encoded image content.
         */
        public String base64() {
            return encodeBase64(content);
        }

        /**
         * Returns the image as a data URI.
         * @return The data URI string in the format {@code data:<media-type>;base64,<data>}.
         */
        public String dataUri() {
            return toImageDataUri(content);
        }
    }

    /**
     * Represents an attached document.
     * @param mediaType The document media type (e.g., "application/pdf").
     * @param content The document content bytes.
     * @param fileName The document file name.
     */
    public final record Document(byte[] content, String mediaType, String fileName) implements Serializable {

        /**
         * Returns the document content as a Base64 encoded string.
         * @return The Base64 encoded document content.
         */
        public String base64() {
            return encodeBase64(content);
        }

        /**
         * Returns the document as a data URI.
         * @return The data URI string in the format {@code data:<media-type>;base64,<data>}.
         */
        public String dataUri() {
            return toDataUri(content);
        }

        /**
         * Returns the extension based on the document's media type.
         * @return The extension based on the document's media type.
         */
        public String extension() {
            return toExtension(mediaType());
        }
    }

    /** The user message. */
    private final String message;
    /** The images. */
    private final List<Image> images;
    /** The documents. */
    private final List<Document> documents;

    private ChatInput(Builder builder) {
        this.message = builder.message;
        this.images = unmodifiableList(builder.images);
        this.documents = unmodifiableList(builder.documents);
    }

    private ChatInput(String message, List<Image> images) {
        this.message = message;
        this.images = unmodifiableList(images);
        this.documents = emptyList();
    }

    /**
     * Gets the user message text.
     * @return The message string.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the list of images associated with this input.
     * @return An unmodifiable list of images, or an empty list if no images are attached.
     */
    public List<Image> getImages() {
        return images;
    }

    /**
     * Gets the list of documents associated with this input.
     * @return An unmodifiable list of documents, or an empty list if no documents are attached.
     */
    public List<Document> getDocuments() {
        return documents;
    }

    /**
     * Returns a copy of this input without any documents.
     * <p>
     * This is useful for providers that handle documents separately from the main chat payload.
     *
     * @return A new {@code ChatInput} containing only the message and images.
     */
    public ChatInput withoutDocuments() {
        return new ChatInput(message, images);
    }

    /**
     * Creates a new builder for constructing {@link ChatInput} instances. For example:
     * <pre>
     * ChatInput input = ChatInput.newBuilder()
     *     .message("What do you see in these images?")
     *     .attach(image1, image2)
     *     .build();
     * </pre>
     *
     * @return A new {@code ChatInput.Builder} instance.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link ChatInput} instances.
     * <p>
     * Use {@link ChatInput#newBuilder()} to obtain a new builder instance.
     */
    public static class Builder {
        private String message;
        private List<Image> images = new ArrayList<>();
        private List<Document> documents = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the user message text.
         * @param message The message string.
         * @return This builder instance for chaining.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Attaches files to this input.
         * <p>
         * Files are automatically classified based on their content:
         * <ul>
         *   <li>Supported image formats (JPEG, PNG, GIF, BMP, WEBP) are added as images and sanitized for AI compatibility.</li>
         *   <li>All other files are added as documents with their media type auto-detected.</li>
         * </ul>
         * @param files The file content bytes to attach.
         * @return This builder instance for chaining.
         */
        public Builder attach(byte[]... files) {
            for (var content : files) {
                if (isSupportedImage(content)) {
                    var sanitized = sanitizeImage(content);
                    var mediaType = toImageMediaType(sanitized);
                    var fileName = "image" + (images.size() + 1) + "." + mediaType.split("/", 2)[0];
                    images.add(new Image(sanitized, mediaType, fileName));
                }
                else {
                    var mediaType = guessMediaType(content);
                    var fileName = "document" + (documents.size() + 1) + "." + toExtension(mediaType);
                    documents.add(new Document(content, mediaType, fileName));
                }
            }

            return this;
        }

        /**
         * Finalizes the configuration and creates a {@link ChatInput} instance.
         * @return A fully configured {@code ChatInput} object.
         * @throws IllegalArgumentException if message is blank.
         */
        public ChatInput build() {
            requireNonBlank(message, "message");
            return new ChatInput(this);
        }
    }
}
