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
 * Computed cost of a single chat call, obtained by applying a {@link ChatPricing} to a {@link ChatUsage}.
 * <p>
 * Costs are broken out per token category so callers can see where the spend went:
 * <ul>
 * <li>{@code inputCost} covers the non-cached portion of {@link ChatUsage#inputTokens()}.</li>
 * <li>{@code cachedInputCost} covers the {@link ChatUsage#cachedInputTokens() cached} portion, billed at the provider's (typically discounted) cache-read rate.
 * {@link BigDecimal#ZERO Zero} when no cached tokens were reported.</li>
 * <li>{@code outputCost} covers all output tokens, including any internal reasoning.</li>
 * </ul>
 * <p>
 * All components are scaled to 10 decimal places, which is well below sub-cent precision for any realistic token pricing. Use
 * {@link BigDecimal#setScale(int, java.math.RoundingMode)} on {@link #totalCost()} if you need to round for display.
 * <p>
 * The {@code currency} is passed through from {@link ChatPricing#currency()} and is purely informational.
 *
 * @param inputCost The cost of non-cached input tokens. Never {@code null}.
 * @param cachedInputCost The cost of cached input tokens, or {@link BigDecimal#ZERO} if none were reported. Never {@code null}.
 * @param outputCost The cost of output tokens (including reasoning). Never {@code null}.
 * @param currency Optional ISO 4217 currency carried over from {@link ChatPricing}; may be {@code null}.
 * @author Bauke Scholtz
 * @since 1.4
 * @see ChatUsage#calculateCost(ChatPricing)
 * @see ChatOptions#getLastCost()
 */
public final record ChatCost(BigDecimal inputCost, BigDecimal cachedInputCost, BigDecimal outputCost, Currency currency) implements Serializable {

    /**
     * Validates the record components.
     *
     * @param inputCost The cost of non-cached input tokens.
     * @param cachedInputCost The cost of cached input tokens.
     * @param outputCost The cost of output tokens.
     * @param currency Optional ISO 4217 currency.
     * @throws NullPointerException if any cost component is {@code null}.
     */
    public ChatCost {
        requireNonNull(inputCost, "inputCost");
        requireNonNull(cachedInputCost, "cachedInputCost");
        requireNonNull(outputCost, "outputCost");
    }

    /**
     * Returns the total cost of the chat call: {@code inputCost + cachedInputCost + outputCost}.
     *
     * @return The total cost; never {@code null}.
     */
    public BigDecimal totalCost() {
        return inputCost.add(cachedInputCost).add(outputCost);
    }

}
