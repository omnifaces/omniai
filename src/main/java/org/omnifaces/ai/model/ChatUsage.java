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

import java.io.Serializable;

/**
 * Token usage reported by the AI provider for a single chat call.
 * <p>
 * Obtained via {@link ChatOptions#getLastUsage()} after a successful chat call.
 * A value of {@code -1} on either component means the provider did not report that count.
 *
 * @param inputTokens The number of tokens consumed by the request (system prompt, conversation history, and user message combined), or {@code -1} if not reported by the provider.
 * @param outputTokens The number of tokens generated in the response, or {@code -1} if not reported by the provider.
 * @author Bauke Scholtz
 * @since 1.3
 * @see ChatOptions#getLastUsage()
 */
public record ChatUsage(int inputTokens, int outputTokens) implements Serializable {

    /**
     * Validates the record components.
     *
     * @param inputTokens The number of input tokens, must be {@code -1} or a positive value.
     * @param outputTokens The number of output tokens, must be {@code -1} or a positive value.
     */
    public ChatUsage {
        if (inputTokens < -1) {
            throw new IllegalArgumentException("Input tokens must be >= -1");
        }
        if (outputTokens < -1) {
            throw new IllegalArgumentException("Output tokens must be >= -1");
        }
    }

    /**
     * Returns the total number of tokens consumed by the chat call (input and output combined),
     * or {@code -1} if either component was not reported by the provider.
     *
     * @return The total token count, or {@code -1} if unavailable.
     */
    public int totalTokens() {
        if (inputTokens == -1 || outputTokens == -1) {
            return -1;
        }

        return inputTokens + outputTokens;
    }
}
