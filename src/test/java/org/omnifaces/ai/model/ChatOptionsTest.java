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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import jakarta.json.Json;

import org.junit.jupiter.api.Test;
import org.omnifaces.ai.model.ChatInput.Message.Role;

class ChatOptionsTest {

    // =================================================================================================================
    // Preset instances tests
    // =================================================================================================================

    @Test
    void preset_default() {
        var options = ChatOptions.DEFAULT;

        assertNotNull(options);
        assertNull(options.getSystemPrompt());
        assertNull(options.getJsonSchema());
        assertEquals(ChatOptions.DEFAULT_TEMPERATURE, options.getTemperature());
        assertNull(options.getMaxTokens());
        assertEquals(ChatOptions.DEFAULT_TOP_P, options.getTopP());
    }

    @Test
    void preset_creative_hasHigherTemperature() {
        assertEquals(ChatOptions.CREATIVE_TEMPERATURE, ChatOptions.CREATIVE.getTemperature());
    }

    @Test
    void preset_deterministic_hasZeroTemperature() {
        assertEquals(ChatOptions.DETERMINISTIC_TEMPERATURE, ChatOptions.DETERMINISTIC.getTemperature());
    }

    // =================================================================================================================
    // Builder tests - validation
    // =================================================================================================================

    @Test
    void builder_temperature_atBoundaries() {
        assertEquals(0.0, ChatOptions.newBuilder().temperature(0.0).build().getTemperature());
        assertEquals(2.0, ChatOptions.newBuilder().temperature(2.0).build().getTemperature());
    }

    @Test
    void builder_temperature_belowZero_throwsException() {
        var builder = ChatOptions.newBuilder();

        var exception = assertThrows(IllegalArgumentException.class, () -> builder.temperature(-0.1));
        assertEquals("Temperature must be between 0.0 and 2.0", exception.getMessage());
    }

    @Test
    void builder_temperature_aboveMax_throwsException() {
        var builder = ChatOptions.newBuilder();

        assertThrows(IllegalArgumentException.class, () -> builder.temperature(2.1));
    }

    @Test
    void builder_maxTokens_positive() {
        assertEquals(1, ChatOptions.newBuilder().maxTokens(1).build().getMaxTokens());
        assertEquals(500, ChatOptions.newBuilder().maxTokens(500).build().getMaxTokens());
    }

    @Test
    void builder_maxTokens_zero_throwsException() {
        var builder = ChatOptions.newBuilder();

        var exception = assertThrows(IllegalArgumentException.class, () -> builder.maxTokens(0));
        assertEquals("Max tokens must be positive", exception.getMessage());
    }

    @Test
    void builder_maxTokens_negative_throwsException() {
        var builder = ChatOptions.newBuilder();

        assertThrows(IllegalArgumentException.class, () -> builder.maxTokens(-1));
    }

    @Test
    void builder_topP_atBoundaries() {
        assertEquals(0.0, ChatOptions.newBuilder().topP(0.0).build().getTopP());
        assertEquals(1.0, ChatOptions.newBuilder().topP(1.0).build().getTopP());
    }

    @Test
    void builder_topP_belowZero_throwsException() {
        var builder = ChatOptions.newBuilder();

        var exception = assertThrows(IllegalArgumentException.class, () -> builder.topP(-0.1));
        assertEquals("Top-P must be between 0.0 and 1.0", exception.getMessage());
    }

    @Test
    void builder_topP_aboveOne_throwsException() {
        var builder = ChatOptions.newBuilder();

        assertThrows(IllegalArgumentException.class, () -> builder.topP(1.1));
    }

    // =================================================================================================================
    // Builder tests - chaining
    // =================================================================================================================

    @Test
    void builder_chaining_allOptions() {
        var schema = Json.createObjectBuilder().add("type", "object").build();

        var options = ChatOptions.newBuilder()
                .systemPrompt("You are helpful.")
                .jsonSchema(schema)
                .temperature(0.5)
                .maxTokens(1000)
                .topP(0.8)
                .build();

        assertEquals("You are helpful.", options.getSystemPrompt());
        assertEquals(schema, options.getJsonSchema());
        assertEquals(0.5, options.getTemperature());
        assertEquals(1000, options.getMaxTokens());
        assertEquals(0.8, options.getTopP());
    }

    // =================================================================================================================
    // Persistent chat tests
    // =================================================================================================================

    @Test
    void preset_default_isNotPersistent() {
        assertFalse(ChatOptions.DEFAULT.isPersistent());
        assertFalse(ChatOptions.CREATIVE.isPersistent());
        assertFalse(ChatOptions.DETERMINISTIC.isPersistent());
    }

    @Test
    void builder_persistent() {
        var options = ChatOptions.newBuilder().persistent().build();

        assertTrue(options.isPersistent());
        assertTrue(options.getHistory().isEmpty());
    }

    @Test
    void builder_nonPersistent_getHistory_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class, options::getHistory);
    }

    @Test
    void builder_nonPersistent_recordMessage_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class, () -> options.recordMessage(Role.USER, "test"));
    }

    @Test
    void persistent_recordMessage_updatesHistory() {
        var options = ChatOptions.newBuilder().persistent().build();

        options.recordMessage(Role.USER, "Hello");
        options.recordMessage(Role.ASSISTANT, "Hi there");

        var history = options.getHistory();
        assertEquals(2, history.size());
        assertEquals(Role.USER, history.get(0).role());
        assertEquals("Hello", history.get(0).content());
        assertEquals(Role.ASSISTANT, history.get(1).role());
        assertEquals("Hi there", history.get(1).content());
    }

    @Test
    void persistent_getHistory_isImmutable() {
        var options = ChatOptions.newBuilder().persistent().build();
        options.recordMessage(Role.USER, "test");

        assertThrows(UnsupportedOperationException.class, () -> options.getHistory().clear());
    }

    // =================================================================================================================
    // Serialization tests
    // =================================================================================================================

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(ChatOptions.class));
    }

    @Test
    void serialization_preservesAllFields() throws Exception {
        var schema = Json.createObjectBuilder().add("type", "object").build();

        var original = ChatOptions.newBuilder()
                .systemPrompt("Test prompt")
                .jsonSchema(schema)
                .temperature(0.8)
                .maxTokens(500)
                .topP(0.7)
                .build();

        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        var bais = new ByteArrayInputStream(baos.toByteArray());
        try (var ois = new ObjectInputStream(bais)) {
            var deserialized = (ChatOptions) ois.readObject();

            assertEquals(original.getSystemPrompt(), deserialized.getSystemPrompt());
            assertNotNull(deserialized.getJsonSchema());
            assertEquals(original.getJsonSchema().toString(), deserialized.getJsonSchema().toString());
            assertEquals(original.getTemperature(), deserialized.getTemperature());
            assertEquals(original.getMaxTokens(), deserialized.getMaxTokens());
            assertEquals(original.getTopP(), deserialized.getTopP());
        }
    }

    @Test
    void serialization_withNullJsonSchema() throws Exception {
        var original = ChatOptions.newBuilder().systemPrompt("Test").build();

        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        var bais = new ByteArrayInputStream(baos.toByteArray());
        try (var ois = new ObjectInputStream(bais)) {
            var deserialized = (ChatOptions) ois.readObject();

            assertNull(deserialized.getJsonSchema());
        }
    }
}
