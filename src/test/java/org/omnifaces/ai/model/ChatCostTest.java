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

class ChatCostTest {

    // =================================================================================================================
    // Constructor tests
    // =================================================================================================================

    @Test
    void new_validCosts_createsRecord() {
        var usd = Currency.getInstance("USD");
        var cost = new ChatCost(new BigDecimal("0.0003"), new BigDecimal("0.0001"), new BigDecimal("0.0075"), usd);

        assertEquals(new BigDecimal("0.0003"), cost.inputCost());
        assertEquals(new BigDecimal("0.0001"), cost.cachedInputCost());
        assertEquals(new BigDecimal("0.0075"), cost.outputCost());
        assertEquals(usd, cost.currency());
    }

    @Test
    void new_nullCurrencyAllowed() {
        var cost = new ChatCost(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);

        assertNull(cost.currency());
    }

    @Test
    void new_nullInputCost_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new ChatCost(null, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    @Test
    void new_nullCachedInputCost_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new ChatCost(BigDecimal.ZERO, null, BigDecimal.ZERO, null));
    }

    @Test
    void new_nullOutputCost_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new ChatCost(BigDecimal.ZERO, BigDecimal.ZERO, null, null));
    }

    // =================================================================================================================
    // totalCost
    // =================================================================================================================

    @Test
    void totalCost_sumsAllComponents() {
        var cost = new ChatCost(new BigDecimal("0.0003"), new BigDecimal("0.0001"), new BigDecimal("0.0075"), Currency.getInstance("USD"));

        assertEquals(new BigDecimal("0.0079"), cost.totalCost());
    }

    @Test
    void totalCost_allZero() {
        var cost = new ChatCost(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);

        assertEquals(BigDecimal.ZERO, cost.totalCost());
    }

    // =================================================================================================================
    // Serialization
    // =================================================================================================================

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(ChatCost.class));
    }

    @Test
    void serialization_roundTripsAllFields() throws Exception {
        var original = new ChatCost(new BigDecimal("0.0003"), new BigDecimal("0.0001"), new BigDecimal("0.0075"), Currency.getInstance("USD"));

        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        try (var ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            var deserialized = (ChatCost) ois.readObject();
            assertEquals(original, deserialized);
        }
    }

}
