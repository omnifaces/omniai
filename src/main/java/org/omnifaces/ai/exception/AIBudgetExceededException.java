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
package org.omnifaces.ai.exception;

import java.math.BigDecimal;
import java.util.Currency;

import org.omnifaces.ai.model.ChatOptions;
import org.omnifaces.ai.model.ChatPricing;

/**
 * Exception thrown when the cumulative cost tracked on a {@link ChatOptions} instance has reached or exceeded the configured cap.
 * <p>
 * Raised by the AI service before dispatching a chat call when {@link ChatOptions#getTotalCost()} is greater than or equal to
 * {@link ChatOptions#getMaxTotalCost()}. The offending call is not made; the caller may catch this exception and reset the counter via
 * {@link ChatOptions#resetBudget()}, switch to a different {@code ChatOptions} instance, or even fail over to a different {@link org.omnifaces.ai.AIService}
 * (e.g. a cheaper model) to continue.
 *
 * @author Bauke Scholtz
 * @since 1.4
 * @see ChatOptions#getMaxTotalCost()
 * @see ChatOptions#getTotalCost()
 * @see ChatOptions#resetBudget()
 */
public class AIBudgetExceededException extends AIException {

    private static final long serialVersionUID = 1L;

    /** The cumulative cost tracked on the {@link ChatOptions} instance. */
    private final BigDecimal totalCost;

    /** The configured cap that has been reached or exceeded. */
    private final BigDecimal maxTotalCost;

    /** The currency inherited from {@link ChatPricing#currency()}, or {@code null} if unspecified. */
    private final Currency currency;

    /**
     * Constructs a new budget exceeded exception.
     *
     * @param totalCost The cumulative cost tracked on the {@link ChatOptions} instance.
     * @param maxTotalCost The configured cap that has been reached or exceeded.
     * @param currency The currency inherited from {@link ChatPricing#currency()}, or {@code null} if unspecified.
     */
    public AIBudgetExceededException(BigDecimal totalCost, BigDecimal maxTotalCost, Currency currency) {
        super(buildMessage(totalCost, maxTotalCost, currency));
        this.totalCost = totalCost;
        this.maxTotalCost = maxTotalCost;
        this.currency = currency;
    }

    private static String buildMessage(BigDecimal totalCost, BigDecimal maxTotalCost, Currency currency) {
        var unit = currency != null ? " " + currency.getCurrencyCode() : "";
        return "Budget exceeded: " + totalCost + unit + " >= cap of " + maxTotalCost + unit;
    }

    /**
     * Returns the cumulative cost tracked on the {@link ChatOptions} instance at the time the exception was raised.
     *
     * @return The total cost.
     */
    public BigDecimal getTotalCost() {
        return totalCost;
    }

    /**
     * Returns the configured cap that has been reached or exceeded.
     *
     * @return The cap.
     */
    public BigDecimal getMaxTotalCost() {
        return maxTotalCost;
    }

    /**
     * Returns the currency carried over from the {@link ChatPricing} on the {@link ChatOptions} instance, or {@code null} if none was configured.
     *
     * @return The currency, or {@code null}.
     */
    public Currency getCurrency() {
        return currency;
    }

}
