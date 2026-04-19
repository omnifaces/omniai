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
import java.util.Currency;

/**
 * Pricing configuration used to turn a {@link ChatUsage} into a {@link ChatCost}.
 * <p>
 * All prices are expressed <em>per one million tokens</em>, matching how AI providers publish their rates. The user supplies the values directly: there are
 * deliberately no built-in presets because provider rates drift over time and differ per model tier. Look up the current rate sheet for your chosen model and
 * pass those numbers in.
 * <p>
 * {@code cachedInputTokenPrice} is the rate applied to the subset of input tokens that was served from the provider's prompt cache (i.e.
 * {@link ChatUsage#cachedInputTokens()}). It is optional; when {@code null}, the full {@code inputTokenPrice} is applied to all input tokens, including cached
 * ones. Set it explicitly to reflect the provider's cache-read discount (e.g. Anthropic charges roughly 10% of the input rate for cache reads, OpenAI and
 * Google charge roughly 25%).
 * <p>
 * {@code outputTokenPrice} applies to all output tokens, including any internal reasoning tokens — providers bill reasoning at the output rate.
 * <p>
 * <strong>Disclaimer:</strong> this class models a simplified three-tier pricing scheme (base input, cached input, output) that covers the common case but does
 * not capture every billing axis some providers expose. Most notably, Anthropic bills cache <em>writes</em> at a premium over the base input rate (roughly
 * 1.25&times; for a 5-minute TTL and 2&times; for a 1-hour TTL), whereas this model bills all non-cached input tokens — writes included — at
 * {@code inputTokenPrice}. For workloads that rely heavily on explicit prompt caching, the resulting {@link ChatCost} may therefore underestimate the actual
 * invoice. Consumers that need strict accuracy should post-process {@link ChatUsage} against the provider's own billing API.
 * <p>
 * The optional {@code currency} is passed through to {@link ChatCost} for display purposes only; it does not affect any arithmetic. Use whatever unit you
 * supplied the prices in (e.g. {@code Currency.getInstance("USD")}).
 *
 * @param inputTokenPrice The price per one million input tokens. Must be non-{@code null} and non-negative.
 * @param cachedInputTokenPrice The price per one million cached input tokens (cache reads), or {@code null} to apply {@code inputTokenPrice} to cached tokens
 * as well. Must be non-negative when set.
 * @param outputTokenPrice The price per one million output tokens (including reasoning). Must be non-{@code null} and non-negative.
 * @param currency Optional ISO 4217 currency for display; may be {@code null}.
 * @author Bauke Scholtz
 * @since 1.4
 * @see ChatOptions.Builder#pricing(ChatPricing)
 * @see ChatOptions#getPricing()
 * @see ChatOptions#getLastCost()
 * @see ChatUsage#calculateCost(ChatPricing)
 */
public final record ChatPricing(BigDecimal inputTokenPrice, BigDecimal cachedInputTokenPrice, BigDecimal outputTokenPrice, Currency currency)
    implements
        Serializable {

    /**
     * Validates the record components.
     *
     * @param inputTokenPrice The price per one million input tokens.
     * @param cachedInputTokenPrice The price per one million cached input tokens, or {@code null}.
     * @param outputTokenPrice The price per one million output tokens.
     * @param currency Optional ISO 4217 currency.
     * @throws NullPointerException if {@code inputTokenPrice} or {@code outputTokenPrice} is {@code null}.
     * @throws IllegalArgumentException if any price is negative.
     */
    public ChatPricing {
        requireNonNull(inputTokenPrice, "inputTokenPrice");
        requireNonNull(outputTokenPrice, "outputTokenPrice");

        if (inputTokenPrice.signum() < 0) {
            throw new IllegalArgumentException("Input token price must be non-negative");
        }
        if (cachedInputTokenPrice != null && cachedInputTokenPrice.signum() < 0) {
            throw new IllegalArgumentException("Cached input token price must be non-negative");
        }
        if (outputTokenPrice.signum() < 0) {
            throw new IllegalArgumentException("Output token price must be non-negative");
        }
    }

    /**
     * Creates a pricing configuration with only input and output token prices. Cached input tokens are billed at the {@code inputTokenPrice}, and no currency
     * label is attached.
     *
     * @param inputTokenPrice The price per one million input tokens.
     * @param outputTokenPrice The price per one million output tokens.
     * @return A new {@link ChatPricing} instance.
     */
    public static ChatPricing of(BigDecimal inputTokenPrice, BigDecimal outputTokenPrice) {
        return new ChatPricing(inputTokenPrice, null, outputTokenPrice, null);
    }

    /**
     * Creates a pricing configuration with distinct cached input pricing but no currency label.
     *
     * @param inputTokenPrice The price per one million input tokens.
     * @param cachedInputTokenPrice The price per one million cached input tokens, or {@code null} to apply {@code inputTokenPrice}.
     * @param outputTokenPrice The price per one million output tokens.
     * @return A new {@link ChatPricing} instance.
     */
    public static ChatPricing of(BigDecimal inputTokenPrice, BigDecimal cachedInputTokenPrice, BigDecimal outputTokenPrice) {
        return new ChatPricing(inputTokenPrice, cachedInputTokenPrice, outputTokenPrice, null);
    }

    /**
     * Returns the price applied to cached input tokens: {@link #cachedInputTokenPrice()} when set, otherwise {@link #inputTokenPrice()}.
     *
     * @return The effective cached input token price; never {@code null}.
     */
    public BigDecimal effectiveCachedInputTokenPrice() {
        return cachedInputTokenPrice != null ? cachedInputTokenPrice : inputTokenPrice;
    }

}
