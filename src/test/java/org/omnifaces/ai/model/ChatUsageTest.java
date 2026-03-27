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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

class ChatUsageTest {

    // =================================================================================================================
    // Constructor tests
    // =================================================================================================================

    @Test
    void new_validTokens_createsRecord() {
        var usage = new ChatUsage(100, 50, 20);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(20, usage.reasoningTokens());
    }

    @Test
    void new_unknownInputTokens_accepted() {
        var usage = new ChatUsage(-1, 50, -1);

        assertEquals(-1, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
    }

    @Test
    void new_unknownOutputTokens_accepted() {
        var usage = new ChatUsage(100, -1, -1);

        assertEquals(100, usage.inputTokens());
        assertEquals(-1, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
    }

    @Test
    void new_unknownReasoningTokens_accepted() {
        var usage = new ChatUsage(100, 50, -1);

        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
    }

    @Test
    void new_allUnknown_accepted() {
        var usage = new ChatUsage(-1, -1, -1);

        assertEquals(-1, usage.inputTokens());
        assertEquals(-1, usage.outputTokens());
        assertEquals(-1, usage.reasoningTokens());
    }

    @Test
    void new_invalidInputTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(-2, 50, -1));
    }

    @Test
    void new_invalidOutputTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(100, -2, -1));
    }

    @Test
    void new_invalidReasoningTokens_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(100, 50, -2));
    }

    @Test
    void new_allInvalid_throwsExceptionOnInput() {
        // compact constructor checks inputTokens first
        assertThrows(IllegalArgumentException.class, () -> new ChatUsage(-5, -5, -5));
    }

    // =================================================================================================================
    // totalTokens tests
    // =================================================================================================================

    @Test
    void totalTokens_bothKnown_returnsSum() {
        // reasoningTokens is a subset of outputTokens, not added separately
        assertEquals(150, new ChatUsage(100, 50, 20).totalTokens());
    }

    @Test
    void totalTokens_noReasoning_returnsInputPlusOutput() {
        assertEquals(150, new ChatUsage(100, 50, -1).totalTokens());
    }

    @Test
    void totalTokens_unknownInput_returnsUnknown() {
        assertEquals(-1, new ChatUsage(-1, 50, -1).totalTokens());
    }

    @Test
    void totalTokens_unknownOutput_returnsUnknown() {
        assertEquals(-1, new ChatUsage(100, -1, -1).totalTokens());
    }

    @Test
    void totalTokens_allUnknown_returnsUnknown() {
        assertEquals(-1, new ChatUsage(-1, -1, -1).totalTokens());
    }

    // =================================================================================================================
    // Equality tests
    // =================================================================================================================

    @Test
    void equality_sameValues_areEqual() {
        assertEquals(new ChatUsage(100, 50, 20), new ChatUsage(100, 50, 20));
    }

    @Test
    void equality_differentInputTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, -1), new ChatUsage(200, 50, -1));
    }

    @Test
    void equality_differentOutputTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, -1), new ChatUsage(100, 75, -1));
    }

    @Test
    void equality_differentReasoningTokens_areNotEqual() {
        assertNotEquals(new ChatUsage(100, 50, 20), new ChatUsage(100, 50, 30));
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
        var original = new ChatUsage(100, 50, 20);

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
        var original = new ChatUsage(-1, -1, -1);

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

}
