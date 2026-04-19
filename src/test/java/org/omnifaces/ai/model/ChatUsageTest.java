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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class ChatUsageTest {

    // =================================================================================================================
    // Constructor tests
    // =================================================================================================================

    @Test
    void new_validTokens_createsRecord() {
        var usage = new ChatUsage(100, 50, 20, 40);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(20, usage.reasoningTokens());
        assertEquals(40, usage.cachedInputTokens());
    }

    @Test
    void new_unknownInputTokens_accepted() {
        var usage = new ChatUsage(-1, 50, -1, -1);

        assertEquals(-1, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
        assertEquals(-1, usage.cachedInputTokens());
    }

    @Test
    void new_unknownOutputTokens_accepted() {
        var usage = new ChatUsage(100, -1, -1, -1);

        assertEquals(100, usage.inputTokens());
        assertEquals(-1, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
        assertEquals(-1, usage.cachedInputTokens());
    }

    @Test
    void new_unknownReasoningTokens_accepted() {
        var usage = new ChatUsage(100, 50, -1, -1);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
        assertEquals(-1, usage.cachedInputTokens());
    }

    @Test
    void new_unknownCachedInputTokens_accepted() {
        var usage = new ChatUsage(100, 50, 20, -1);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(20, usage.reasoningTokens());
        assertEquals(-1, usage.cachedInputTokens());
    }

    @Test
    void new_allUnknown_accepted() {
        var usage = new ChatUsage(-1, -1, -1, -1);

        assertEquals(-1, usage.inputTokens());
        assertEquals(-1, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
        assertEquals(-1, usage.cachedInputTokens());
    }

    @Test
    void new_invalidInputTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(-2, 50, -1, -1));
    }

    @Test
    void new_invalidOutputTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(100, -2, -1, -1));
    }

    @Test
    void new_invalidReasoningTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(100, 50, -2, -1));
    }

    @Test
    void new_invalidCachedInputTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(100, 50, -1, -2));
    }

    @Test
    void new_allInvalid_throwsExceptionOnInput() {
        // compact constructor checks inputTokens first
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(-5, -5, -5, -5));
    }

    // =================================================================================================================
    // totalTokens tests
    // =================================================================================================================

    @Test
    void totalTokens_bothKnown_returnsSum() {
        // reasoningTokens is a subset of outputTokens, not added separately
        assertEquals(150, new ChatUsage(100, 50, 20, -1).totalTokens());
    }

    @Test
    void totalTokens_noReasoning_returnsInputPlusOutput() {
        assertEquals(150, new ChatUsage(100, 50, -1, -1).totalTokens());
    }

    @Test
    void totalTokens_cachedSubsetDoesNotAffectTotal() {
        // cachedInputTokens is a subset of inputTokens, already counted; must not be added separately
        assertEquals(150, new ChatUsage(100, 50, -1, 80).totalTokens());
    }

    @Test
    void totalTokens_unknownInput_returnsUnknown() {
        assertEquals(-1, new ChatUsage(-1, 50, -1, -1).totalTokens());
    }

    @Test
    void totalTokens_unknownOutput_returnsUnknown() {
        assertEquals(-1, new ChatUsage(100, -1, -1, -1).totalTokens());
    }

    @Test
    void totalTokens_allUnknown_returnsUnknown() {
        assertEquals(-1, new ChatUsage(-1, -1, -1, -1).totalTokens());
    }

    // =================================================================================================================
    // Equality tests
    // =================================================================================================================

    @Test
    void equality_sameValues_areEqual() {
        assertEquals(new ChatUsage(100, 50, 20, 40), new ChatUsage(100, 50, 20, 40));
    }

    @Test
    void equality_differentInputTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, -1, -1), new ChatUsage(200, 50, -1, -1));
    }

    @Test
    void equality_differentOutputTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, -1, -1), new ChatUsage(100, 75, -1, -1));
    }

    @Test
    void equality_differentReasoningTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, 20, -1), new ChatUsage(100, 50, 30, -1));
    }

    @Test
    void equality_differentCachedInputTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, 20, 40), new ChatUsage(100, 50, 20, 50));
    }

    // =================================================================================================================
    // Serialization tests
    // =================================================================================================================

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(ChatUsage.class));
    }

    @Test
    void serialization_preservesKnownValues() throws Exception {
        var original = new ChatUsage(100, 50, 20, 40);

        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        var bais = new ByteArrayInputStream(baos.toByteArray());
        try (var ois = new ObjectInputStream(bais)) {
            var deserialized = (ChatUsage) ois.readObject();

            assertEquals(original, deserialized);
        }
    }

    @Test
    void serialization_preservesUnknownValues() throws Exception {
        var original = new ChatUsage(-1, -1, -1, -1);

        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        var bais = new ByteArrayInputStream(baos.toByteArray());
        try (var ois = new ObjectInputStream(bais)) {
            var deserialized = (ChatUsage) ois.readObject();

            assertEquals(original, deserialized);
        }
    }

    // =================================================================================================================
    // calculateCost tests
    // =================================================================================================================

    @Test
    void calculateCost_allKnown_returnsExpectedComponents() {
        // 1_000_000 input @ $3.00/M, 500_000 cached @ $0.30/M, 2_000_000 output @ $15.00/M
        // non-cached = 500_000, so input cost = 500_000 * 3 / 1_000_000 = 1.5
        // cached cost = 500_000 * 0.30 / 1_000_000 = 0.15
        // output cost = 2_000_000 * 15 / 1_000_000 = 30
        var usage = new ChatUsage(1_000_000, 2_000_000, -1, 500_000);
        var usd = Currency.getInstance("USD");
        var pricing = new ChatPricing(new BigDecimal("3.00"), new BigDecimal("0.30"), new BigDecimal("15.00"), usd);

        var cost = usage.calculateCost(pricing);

        assertEquals(0, new BigDecimal("1.5").compareTo(cost.inputCost()));
        assertEquals(0, new BigDecimal("0.15").compareTo(cost.cachedInputCost()));
        assertEquals(0, new BigDecimal("30").compareTo(cost.outputCost()));
        assertEquals(usd, cost.currency());
    }

    @Test
    void calculateCost_scaleIsTen() {
        var usage = new ChatUsage(1, 1, -1, -1);
        var pricing = ChatPricing.of(new BigDecimal("1.00"), new BigDecimal("1.00"));

        var cost = usage.calculateCost(pricing);

        assertEquals(10, cost.inputCost().scale());
        assertEquals(10, cost.cachedInputCost().scale());
        assertEquals(10, cost.outputCost().scale());
    }

    @Test
    void calculateCost_noCachedPrice_fallsBackToInputPrice() {
        // cached portion billed at inputTokenPrice when cachedInputTokenPrice is null
        var usage = new ChatUsage(1_000_000, 0, -1, 400_000);
        var pricing = ChatPricing.of(new BigDecimal("2.00"), new BigDecimal("10.00"));

        var cost = usage.calculateCost(pricing);

        // cached: 400_000 * 2 / 1_000_000 = 0.8
        // non-cached input: 600_000 * 2 / 1_000_000 = 1.2
        assertEquals(0, new BigDecimal("0.8").compareTo(cost.cachedInputCost()));
        assertEquals(0, new BigDecimal("1.2").compareTo(cost.inputCost()));
    }

    @Test
    void calculateCost_cachedUnreported_treatedAsZero() {
        var usage = new ChatUsage(1_000_000, 500_000, -1, -1);
        var pricing = new ChatPricing(new BigDecimal("2.00"), new BigDecimal("0.20"), new BigDecimal("8.00"), null);

        var cost = usage.calculateCost(pricing);

        // full input at non-cached rate: 1_000_000 * 2 / 1M = 2
        assertEquals(0, new BigDecimal("2").compareTo(cost.inputCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cost.cachedInputCost()));
        assertEquals(0, new BigDecimal("4").compareTo(cost.outputCost()));
    }

    @Test
    void calculateCost_zeroTokens_zeroCost() {
        var usage = new ChatUsage(0, 0, -1, 0);
        var pricing = new ChatPricing(new BigDecimal("3.00"), new BigDecimal("0.30"), new BigDecimal("15.00"), null);

        var cost = usage.calculateCost(pricing);

        assertEquals(0, BigDecimal.ZERO.compareTo(cost.inputCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cost.cachedInputCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cost.outputCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cost.totalCost()));
    }

    @Test
    void calculateCost_inputUnreported_returnsNull() {
        var usage = new ChatUsage(-1, 50, -1, -1);
        var pricing = ChatPricing.of(new BigDecimal("3.00"), new BigDecimal("15.00"));

        assertNull(usage.calculateCost(pricing));
    }

    @Test
    void calculateCost_outputUnreported_returnsNull() {
        var usage = new ChatUsage(100, -1, -1, -1);
        var pricing = ChatPricing.of(new BigDecimal("3.00"), new BigDecimal("15.00"));

        assertNull(usage.calculateCost(pricing));
    }

    @Test
    void calculateCost_nullPricing_throwsNPE() {
        var usage = new ChatUsage(100, 50, -1, -1);

        assertThrows(NullPointerException.class, () -> usage.calculateCost(null));
    }

    @Test
    void calculateCost_subCentPrecision() {
        // 50 tokens at $3/M = 0.00015
        var usage = new ChatUsage(50, 0, -1, -1);
        var pricing = ChatPricing.of(new BigDecimal("3.00"), new BigDecimal("15.00"));

        var cost = usage.calculateCost(pricing);

        assertEquals(0, new BigDecimal("0.00015").compareTo(cost.inputCost()));
    }

}
