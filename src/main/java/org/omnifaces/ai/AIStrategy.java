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
package org.omnifaces.ai;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.io.Serializable;

/**
 * Strategy for AI services.
 * <p>
 * Use the static {@code of(...)} factory methods to create instances with non-null handlers,
 * or the canonical constructor directly when null handlers are acceptable (the corresponding
 * AI service implementation will then use its default handler).
 *
 * @param textHandler The text handler.
 * @param imageHandler The image handler
 * @see AIService
 * @see AITextHandler
 * @see AIImageHandler
 * @author Bauke Scholtz
 * @since 1.0
 */
public final record AIStrategy(AITextHandler textHandler, AIImageHandler imageHandler) implements Serializable {

    /**
     * Creates a new {@code AIStrategy} with only a text handler.
     * The image handler will be {@code null}, causing the AI service to use its default image handler.
     *
     * @param textHandler The text handler (must not be {@code null}).
     * @return A new strategy instance with the specified text handler.
     * @throws NullPointerException If {@code textHandler} is {@code null}.
     */
    public static AIStrategy of(AITextHandler textHandler) {
        requireNonNull(textHandler, "textHandler");
        return new AIStrategy(textHandler, null);
    }

    /**
     * Creates a new {@code AIStrategy} with only an image handler.
     * The text handler will be {@code null}, causing the AI service to use its default text handler.
     *
     * @param imageHandler The image handler (must not be {@code null}).
     * @return A new strategy instance with the specified image handler.
     * @throws NullPointerException If {@code imageHandler} is {@code null}.
     */
    public static AIStrategy of(AIImageHandler imageHandler) {
        requireNonNull(imageHandler, "imageHandler");
        return new AIStrategy(null, imageHandler);
    }

    /**
     * Creates a new {@code AIStrategy} with both a text handler and an image handler.
     *
     * @param textHandler The text handler (must not be {@code null}).
     * @param imageHandler The image handler (must not be {@code null}).
     * @return A new strategy instance with the specified handlers.
     * @throws NullPointerException If {@code textHandler} or {@code imageHandler} is {@code null}.
     */
    public static AIStrategy of(AITextHandler textHandler, AIImageHandler imageHandler) {
        requireNonNull(textHandler, "textHandler");
        requireNonNull(imageHandler, "imageHandler");
        return new AIStrategy(textHandler, imageHandler);
    }

    /**
     * Returns a new {@code AIStrategy} with any {@code null} handlers replaced by the specified defaults.
     *
     * @param defaultTextHandler The default text handler to use if this strategy's text handler is {@code null}.
     * @param defaultImageHandler The default image handler to use if this strategy's image handler is {@code null}.
     * @return A new strategy instance with non-null handlers.
     */
    public AIStrategy withDefaults(AITextHandler defaultTextHandler, AIImageHandler defaultImageHandler) {
        return new AIStrategy(ofNullable(textHandler).orElse(defaultTextHandler), ofNullable(imageHandler).orElse(defaultImageHandler));
    }

}
