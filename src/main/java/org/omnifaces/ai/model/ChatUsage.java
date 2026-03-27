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
 * Obtained via {@link ChatOptions#getLastUsage()} after a successful chat call. A value of {@code -1} on any component means the provider did not report that
 * count.
 * <p>
 * {@code outputTokens} always represents the total number of non-input tokens generated, including any internal reasoning or thinking tokens.
 * {@code reasoningTokens} exposes the reasoning subset for informational purposes and is always {@code <= outputTokens} when both are known.
 *
 * @param inputTokens The number of tokens consumed by the request (system prompt, conversation history, and user message combined), or {@code -1} if not
 * reported by the provider.
 * @param outputTokens The number of tokens generated in the response, including any internal reasoning/thinking tokens, or {@code -1} if not reported by the
 * provider.
 * @param reasoningTokens The number of tokens used for internal reasoning/thinking, or {@code -1} if the provider does not report this separately. Always a
 * subset of {@code outputTokens} when known.
 * @author Bauke Scholtz
 * @since 1.3
 * @see ChatOptions#getLastUsage()
 */
public final record ChatUsage(int inputTokens, int outputTokens, int reasoningTokens) implements Serializable {

    /**
     * Validates the record components.
     *
     * @param inputTokens The number of tokens consumed by the request (system prompt, conversation history, and user message combined), must be {@code -1} or a
     * non-negative value.
     * @param outputTokens The number of tokens generated in the response, including any internal reasoning/thinking tokens, must be {@code -1} or a
     * non-negative value.
     * @param reasoningTokens The number of tokens used for internal reasoning/thinking, must be {@code -1} or a non-negative value. Always a subset of
     * {@code outputTokens} when known.
     */
    public ChatUsage {
        if (inputTokens < -1) {
            throw new IllegalArgumentException("Input tokens must be >= -1");
        }
        if (outputTokens < -1) {
            throw new IllegalArgumentException("Output tokens must be >= -1");
        }
        if (reasoningTokens < -1) {
            throw new IllegalArgumentException("Reasoning tokens must be >= -1");
        }
    }

    /**
     * Returns the total number of tokens consumed by the chat call (input and output combined), or {@code -1} if either component was not reported by the
     * provider.
     * <p>
     * Since {@code outputTokens} already includes any reasoning tokens, this method does not add {@code reasoningTokens} separately to avoid double-counting.
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
