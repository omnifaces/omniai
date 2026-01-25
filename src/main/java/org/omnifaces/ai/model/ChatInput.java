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

import static org.omnifaces.ai.helper.TextHelper.isBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    /** The user message. */
    private final String message;
    /** The images. */
    private final List<byte[]> images;

    private ChatInput(Builder builder) {
        this.message = builder.message;
        this.images = Collections.unmodifiableList(new ArrayList<>(builder.images));
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
     * <p>
     * Each image is represented as a byte array containing the image data.
     *
     * @return An unmodifiable list of image byte arrays, or an empty list if no images are attached.
     */
    public List<byte[]> getImages() {
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
        private List<byte[]> images = new ArrayList<>();

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
         * Sets the images for this input, replacing any previously added images.
         * <p>
         * Each image should be provided as a byte array containing the image data.
         *
         * @param images The image byte arrays to include.
         * @return This builder instance for chaining.
         */
        public Builder images(byte[]... images) {
            this.images = new ArrayList<>(Arrays.asList(images));
            return this;
        }

        /**
         * Finalizes the configuration and creates a {@link ChatInput} instance.
         *
         * @return A fully configured {@code ChatInput} object.
         * @throws IllegalArgumentException if message is blank.
         */
        public ChatInput build() {
            if (isBlank(message)) {
                throw new IllegalArgumentException("Message cannot be blank");
            }

            return new ChatInput(this);
        }
    }
}
