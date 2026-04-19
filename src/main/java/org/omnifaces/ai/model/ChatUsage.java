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

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Token usage reported by the AI provider for a single chat call.
 * <p>
 * Obtained via {@link ChatOptions#getLastUsage()} after a successful chat call. A value of {@code -1} on any component means the provider did not report that
 * count.
 * <p>
 * {@code outputTokens} always represents the total number of non-input tokens generated, including any internal reasoning or thinking tokens.
 * {@code reasoningTokens} exposes the reasoning subset for informational purposes and is always {@code <= outputTokens} when both are known.
 * <p>
 * {@code cachedInputTokens} exposes the portion of {@code inputTokens} that was served from the provider's prompt cache and is always {@code <= inputTokens}
 * when both are known. It is populated when the provider supports and reports prompt caching (Anthropic: {@code cache_read_input_tokens}, OpenAI:
 * {@code cached_tokens}, Google: {@code cachedContentTokenCount}). Automatically activated on memory-enabled chat calls for providers that require explicit
 * cache markers; fully automatic for providers that cache implicitly.
 *
 * @param inputTokens The number of tokens consumed by the request (system prompt, conversation history, and user message combined), or {@code -1} if not
 * reported by the provider.
 * @param outputTokens The number of tokens generated in the response, including any internal reasoning/thinking tokens, or {@code -1} if not reported by the
 * provider.
 * @param reasoningTokens The number of tokens used for internal reasoning/thinking, or {@code -1} if the provider does not report this separately. Always a
 * subset of {@code outputTokens} when known.
 * @param cachedInputTokens The subset of {@code inputTokens} served from the provider's prompt cache, or {@code -1} if the provider does not report this.
 * Always a subset of {@code inputTokens} when known.
 * @author Bauke Scholtz
 * @since 1.3
 * @see ChatOptions#getLastUsage()
 */
public final record ChatUsage(int inputTokens, int outputTokens, int reasoningTokens, int cachedInputTokens) implements Serializable {

    /** One million, used as the denominator when converting per-million-tokens pricing to per-call costs. */
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    /** Scale applied to computed {@link ChatCost} components. Ten decimal places is well below sub-cent precision for any realistic token pricing. */
    private static final int COST_SCALE = 10;

    /**
     * Validates the record components.
     *
     * @param inputTokens The number of tokens consumed by the request (system prompt, conversation history, and user message combined), must be {@code -1} or a
     * non-negative value.
     * @param outputTokens The number of tokens generated in the response, including any internal reasoning/thinking tokens, must be {@code -1} or a
     * non-negative value.
     * @param reasoningTokens The number of tokens used for internal reasoning/thinking, must be {@code -1} or a non-negative value. Always a subset of
     * {@code outputTokens} when known.
     * @param cachedInputTokens The subset of {@code inputTokens} served from the provider's prompt cache, must be {@code -1} or a non-negative value. Always a
     * subset of {@code inputTokens} when known.
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
        if (cachedInputTokens < -1) {
            throw new IllegalArgumentException("Cached input tokens must be >= -1");
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

    /**
     * Computes the {@link ChatCost} of this usage using the given {@link ChatPricing}.
     * <p>
     * Returns {@code null} when either {@link #inputTokens()} or {@link #outputTokens()} is {@code -1} (unreported by the provider), since a meaningful cost
     * cannot be calculated without both. When {@link #cachedInputTokens()} is {@code -1} it is treated as zero, so the full input is billed at
     * {@link ChatPricing#inputTokenPrice()}.
     * <p>
     * The cached portion of the input is billed at {@link ChatPricing#effectiveCachedInputTokenPrice()} and the non-cached portion at
     * {@link ChatPricing#inputTokenPrice()}. Output tokens (including any internal reasoning) are billed at {@link ChatPricing#outputTokenPrice()}. All prices
     * are interpreted as per one million tokens.
     *
     * @param pricing The pricing configuration. Must not be {@code null}.
     * @return The computed {@link ChatCost}, or {@code null} if input or output tokens were not reported.
     * @throws NullPointerException if {@code pricing} is {@code null}.
     * @since 1.4
     * @see ChatOptions#getLastCost()
     */
    public ChatCost calculateCost(ChatPricing pricing) {
        requireNonNull(pricing, "pricing");

        if (inputTokens == -1 || outputTokens == -1) {
            return null;
        }

        var cached = Math.max(cachedInputTokens, 0);
        var nonCachedInput = inputTokens - cached;

        var inputCost = pricing.inputTokenPrice()
            .multiply(BigDecimal.valueOf(nonCachedInput))
            .divide(MILLION, COST_SCALE, RoundingMode.HALF_UP);

        var cachedInputCost = pricing.effectiveCachedInputTokenPrice()
            .multiply(BigDecimal.valueOf(cached))
            .divide(MILLION, COST_SCALE, RoundingMode.HALF_UP);

        var outputCost = pricing.outputTokenPrice()
            .multiply(BigDecimal.valueOf(outputTokens))
            .divide(MILLION, COST_SCALE, RoundingMode.HALF_UP);

        return new ChatCost(inputCost, cachedInputCost, outputCost, pricing.currency());
    }

}
