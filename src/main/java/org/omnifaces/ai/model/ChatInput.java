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

import static java.util.Collections.unmodifiableList;
import static org.omnifaces.ai.helper.ImageHelper.isSupportedImage;
import static org.omnifaces.ai.helper.ImageHelper.sanitizeImage;
import static org.omnifaces.ai.helper.ImageHelper.toImageDataUri;
import static org.omnifaces.ai.helper.ImageHelper.toImageMediaType;
import static org.omnifaces.ai.helper.TextHelper.encodeBase64;
import static org.omnifaces.ai.helper.TextHelper.requireNonBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.omnifaces.ai.exception.AIException;

/**
 * Input for chat-based AI interactions.
 * <p>
 * This class encapsulates the user input for AI chat operations, including the text message and optional images.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public class ChatInput implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Represents an attached image.
     * @param mediaType The image media type.
     * @param content The image content.
     */
    public final record Image(String mediaType, byte[] content) implements Serializable {

        /**
         *
         * @return
         */
        public String base64() {
            return encodeBase64(content);
        }

        /**
         *
         * @return
         */
        public String dataUri() {
            return toImageDataUri(content);
        }
    }

    /** The user message. */
    private final String message;
    /** The images. */
    private final List<Image> images;

    private ChatInput(Builder builder) {
        this.message = builder.message;
        this.images = unmodifiableList(builder.images);
    }

    /**
     * Gets the user message text.
     *
     * @return The message string.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the list of images associated with this input.
     *
     * @return An unmodifiable list of images, or an empty list if no images are attached.
     */
    public List<Image> getImages() {
        return images;
    }

    /**
     * Creates a new builder for constructing {@link ChatInput} instances. For example:
     * <pre>
     * ChatInput input = ChatInput.newBuilder()
     *     .message("What do you see in these images?")
     *     .images(imageBytes1, imageBytes2)
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

        private Builder() {}

        /**
         * Sets the user message text.
         *
         * @param message The message string.
         * @return This builder instance for chaining.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Attaches the files for this input.
         * <p>
         * Each file should be provided as a byte array containing the data.
         *
         * @param files The files to attach.
         * @return This builder instance for chaining.
         * @throws AIException if the file's mime type is not supported.
         */
        public Builder attach(byte[]... files) {
            for (var file : files) {
                if (isSupportedImage(file)) {
                    var sanitized = sanitizeImage(file);
                    images.add(new Image(toImageMediaType(sanitized), sanitized));
                }
                else {
                    throw new AIException("Unsupported file mime type.");
                }
            }

            return this;
        }

        /**
         * Finalizes the configuration and creates a {@link ChatInput} instance.
         *
         * @return A fully configured {@code ChatInput} object.
         * @throws IllegalArgumentException if message is blank.
         */
        public ChatInput build() {
            requireNonBlank(message, "message");
            return new ChatInput(this);
        }
    }
}
