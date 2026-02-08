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
import org.omnifaces.ai.mime.MimeType;
import org.omnifaces.ai.model.ChatOptions.Message;
import org.omnifaces.ai.model.ChatOptions.Message.Role;
import org.omnifaces.ai.model.ChatOptions.UploadedFile;

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
        assertFalse(ChatOptions.DEFAULT.hasMemory());
        assertFalse(ChatOptions.CREATIVE.hasMemory());
        assertFalse(ChatOptions.DETERMINISTIC.hasMemory());
    }

    @Test
    void builder_withMemory() {
        var options = ChatOptions.newBuilder().withMemory().build();

        assertTrue(options.hasMemory());
        assertTrue(options.getHistory().isEmpty());
    }

    @Test
    void builder_nonMemory_getHistory_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class, options::getHistory);
    }

    @Test
    void builder_nonMemory_recordMessage_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class, () -> options.recordMessage(Role.USER, "test"));
    }

    @Test
    void withMemory_recordMessage_updatesHistory() {
        var options = ChatOptions.newBuilder().withMemory().build();

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
    void withMemory_getHistory_isImmutable() {
        var options = ChatOptions.newBuilder().withMemory().build();
        options.recordMessage(Role.USER, "test");

        assertThrows(UnsupportedOperationException.class, () -> options.getHistory().clear());
    }

    @Test
    void builder_nonMemory_getMaxHistory_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class, options::getMaxHistory);
    }

    @Test
    void withMemory_defaultMaxHistory() {
        var options = ChatOptions.newBuilder().withMemory().build();

        assertEquals(ChatOptions.DEFAULT_MAX_HISTORY, options.getMaxHistory());
    }

    @Test
    void withMemory_customMaxHistory() {
        var options = ChatOptions.newBuilder().withMemory(10).build();

        assertTrue(options.hasMemory());
        assertEquals(10, options.getMaxHistory());
    }

    @Test
    void withMemory_customMaxHistory_zero_throwsException() {
        var builder = ChatOptions.newBuilder();

        var exception = assertThrows(IllegalArgumentException.class, () -> builder.withMemory(0));
        assertEquals("Max history must be positive", exception.getMessage());
    }

    @Test
    void withMemory_customMaxHistory_negative_throwsException() {
        var builder = ChatOptions.newBuilder();

        assertThrows(IllegalArgumentException.class, () -> builder.withMemory(-1));
    }

    @Test
    void withMemory_slidingWindow_dropsOldestMessages() {
        var options = ChatOptions.newBuilder().withMemory(4).build();

        options.recordMessage(Role.USER, "msg1");
        options.recordMessage(Role.ASSISTANT, "reply1");
        options.recordMessage(Role.USER, "msg2");
        options.recordMessage(Role.ASSISTANT, "reply2");

        assertEquals(4, options.getHistory().size());

        options.recordMessage(Role.USER, "msg3");
        options.recordMessage(Role.ASSISTANT, "reply3");

        var history = options.getHistory();
        assertEquals(4, history.size());
        assertEquals("msg2", history.get(0).content());
        assertEquals("reply2", history.get(1).content());
        assertEquals("msg3", history.get(2).content());
        assertEquals("reply3", history.get(3).content());
    }

    // =================================================================================================================
    // withJsonSchema tests
    // =================================================================================================================

    @Test
    void withJsonSchema_copiesAllFields() {
        var original = ChatOptions.newBuilder()
                .systemPrompt("Test prompt")
                .temperature(0.5)
                .maxTokens(500)
                .topP(0.8)
                .build();

        var schema = Json.createObjectBuilder().add("type", "object").build();
        var copy = original.withJsonSchema(schema);

        assertEquals("Test prompt", copy.getSystemPrompt());
        assertEquals(schema, copy.getJsonSchema());
        assertEquals(0.5, copy.getTemperature());
        assertEquals(500, copy.getMaxTokens());
        assertEquals(0.8, copy.getTopP());
    }

    @Test
    void withJsonSchema_setsNewSchema_originalUnchanged() {
        var original = ChatOptions.newBuilder().build();
        var schema = Json.createObjectBuilder().add("type", "object").build();

        var copy = original.withJsonSchema(schema);

        assertNull(original.getJsonSchema());
        assertEquals(schema, copy.getJsonSchema());
    }

    @Test
    void withJsonSchema_sharesMemory() {
        var original = ChatOptions.newBuilder().withMemory().build();
        var schema = Json.createObjectBuilder().add("type", "object").build();
        var copy = original.withJsonSchema(schema);

        assertTrue(copy.hasMemory());
        copy.recordMessage(Role.USER, "Hello");

        assertEquals(1, original.getHistory().size());
        assertEquals("Hello", original.getHistory().get(0).content());
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

    // =================================================================================================================
    // Message record tests
    // =================================================================================================================

    @Test
    void message_validConstruction() {
        var message = new Message(Role.USER, "Hello");

        assertEquals(Role.USER, message.role());
        assertEquals("Hello", message.content());
    }

    @Test
    void message_assistantRole() {
        var message = new Message(Role.ASSISTANT, "Hi there");

        assertEquals(Role.ASSISTANT, message.role());
        assertEquals("Hi there", message.content());
    }

    @Test
    void message_nullRole_throwsException() {
        assertThrows(NullPointerException.class, () -> new Message(null, "Hello"));
    }

    @Test
    void message_nullContent_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Message(Role.USER, null));
    }

    @Test
    void message_blankContent_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Message(Role.USER, ""));
        assertThrows(IllegalArgumentException.class, () -> new Message(Role.USER, "   "));
        assertThrows(IllegalArgumentException.class, () -> new Message(Role.USER, "\t\n"));
    }

    @Test
    void message_implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(Message.class));
    }

    // =================================================================================================================
    // UploadedFile record tests
    // =================================================================================================================

    private static final MimeType TEST_PDF = new MimeType() {
        @Override public String value() { return "application/pdf"; }
        @Override public String extension() { return "pdf"; }
    };

    private static final MimeType TEST_PNG = new MimeType() {
        @Override public String value() { return "image/png"; }
        @Override public String extension() { return "png"; }
    };

    @Test
    void uploadedFile_validConstruction() {
        var uploadedFile = new UploadedFile("file-123", TEST_PDF);

        assertEquals("file-123", uploadedFile.id());
        assertEquals(TEST_PDF, uploadedFile.mimeType());
    }

    @Test
    void uploadedFile_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new UploadedFile(null, TEST_PDF));
    }

    @Test
    void uploadedFile_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new UploadedFile("", TEST_PDF));
        assertThrows(IllegalArgumentException.class, () -> new UploadedFile("   ", TEST_PDF));
    }

    @Test
    void uploadedFile_nullMimeType_throwsException() {
        assertThrows(NullPointerException.class, () -> new UploadedFile("file-123", null));
    }

    @Test
    void uploadedFile_implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(UploadedFile.class));
    }

    // =================================================================================================================
    // getHistory - returns full history
    // =================================================================================================================

    @Test
    void getHistory_returnsAllRecordedMessages() {
        var options = ChatOptions.newBuilder().withMemory().build();

        options.recordMessage(Role.USER, "msg1");
        options.recordMessage(Role.ASSISTANT, "reply1");
        options.recordMessage(Role.USER, "msg2");

        var history = options.getHistory();
        assertEquals(3, history.size());
        assertEquals(Role.USER, history.get(0).role());
        assertEquals("msg1", history.get(0).content());
        assertEquals(Role.ASSISTANT, history.get(1).role());
        assertEquals("reply1", history.get(1).content());
        assertEquals(Role.USER, history.get(2).role());
        assertEquals("msg2", history.get(2).content());
    }

    // =================================================================================================================
    // recordUploadedFile tests
    // =================================================================================================================

    @Test
    void recordUploadedFile_associatesWithLastUserMessage() {
        var options = ChatOptions.newBuilder().withMemory().build();

        options.recordMessage(Role.USER, "Analyze this file");
        options.recordUploadedFile("file-123", TEST_PDF);

        var userMessage = new Message(Role.USER, "Analyze this file");
        var uploadedFiles = options.getUploadedFileHistory(userMessage);
        assertEquals(1, uploadedFiles.size());
        assertEquals("file-123", uploadedFiles.get(0).id());
        assertEquals(TEST_PDF, uploadedFiles.get(0).mimeType());
    }

    @Test
    void recordUploadedFile_multipleFilesOnSameMessage() {
        var options = ChatOptions.newBuilder().withMemory().build();

        options.recordMessage(Role.USER, "Analyze these files");
        options.recordUploadedFile("file-1", TEST_PDF);
        options.recordUploadedFile("file-2", TEST_PNG);

        var userMessage = new Message(Role.USER, "Analyze these files");
        var uploadedFiles = options.getUploadedFileHistory(userMessage);
        assertEquals(2, uploadedFiles.size());
        assertEquals("file-1", uploadedFiles.get(0).id());
        assertEquals("file-2", uploadedFiles.get(1).id());
    }

    @Test
    void recordUploadedFile_findsLastUserMessageAfterAssistant() {
        var options = ChatOptions.newBuilder().withMemory().build();

        options.recordMessage(Role.USER, "First message");
        options.recordMessage(Role.ASSISTANT, "Reply");
        options.recordMessage(Role.USER, "Second message");
        options.recordUploadedFile("file-123", TEST_PDF);

        var firstMessage = new Message(Role.USER, "First message");
        var secondMessage = new Message(Role.USER, "Second message");
        assertTrue(options.getUploadedFileHistory(firstMessage).isEmpty());
        assertEquals(1, options.getUploadedFileHistory(secondMessage).size());
    }

    @Test
    void recordUploadedFile_noUserMessage_throwsException() {
        var options = ChatOptions.newBuilder().withMemory().build();

        assertThrows(IllegalStateException.class, () -> options.recordUploadedFile("file-123", TEST_PDF));
    }

    @Test
    void recordUploadedFile_nonMemory_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class, () -> options.recordUploadedFile("file-123", TEST_PDF));
    }

    // =================================================================================================================
    // getUploadedFileHistory tests
    // =================================================================================================================

    @Test
    void getUploadedFileHistory_noFilesForMessage_returnsEmpty() {
        var options = ChatOptions.newBuilder().withMemory().build();
        options.recordMessage(Role.USER, "No files here");

        var userMessage = new Message(Role.USER, "No files here");
        assertTrue(options.getUploadedFileHistory(userMessage).isEmpty());
    }

    @Test
    void getUploadedFileHistory_isImmutable() {
        var options = ChatOptions.newBuilder().withMemory().build();
        options.recordMessage(Role.USER, "Test");
        options.recordUploadedFile("file-1", TEST_PDF);

        var userMessage = new Message(Role.USER, "Test");
        assertThrows(UnsupportedOperationException.class,
                () -> options.getUploadedFileHistory(userMessage).add(new UploadedFile("file-2", TEST_PNG)));
    }

    @Test
    void getUploadedFileHistory_nonMemory_throwsException() {
        var options = ChatOptions.newBuilder().build();

        assertThrows(IllegalStateException.class,
                () -> options.getUploadedFileHistory(new Message(Role.USER, "test")));
    }

    // =================================================================================================================
    // Sliding window - uploaded file cleanup
    // =================================================================================================================

    @Test
    void slidingWindow_removesUploadedFilesWithEvictedMessages() {
        var options = ChatOptions.newBuilder().withMemory(4).build();

        options.recordMessage(Role.USER, "msg1");
        options.recordUploadedFile("file-1", TEST_PDF);
        options.recordMessage(Role.ASSISTANT, "reply1");
        options.recordMessage(Role.USER, "msg2");
        options.recordUploadedFile("file-2", TEST_PNG);
        options.recordMessage(Role.ASSISTANT, "reply2");

        // History is now full (4 messages). Verify files are accessible.
        var msg1 = new Message(Role.USER, "msg1");
        var msg2 = new Message(Role.USER, "msg2");
        assertEquals(1, options.getUploadedFileHistory(msg1).size());
        assertEquals(1, options.getUploadedFileHistory(msg2).size());

        // Add two more messages, causing msg1 and reply1 to be evicted.
        options.recordMessage(Role.USER, "msg3");
        options.recordMessage(Role.ASSISTANT, "reply3");

        // msg1's uploaded file should be cleaned up.
        assertTrue(options.getUploadedFileHistory(msg1).isEmpty());
        // msg2's uploaded file should still be accessible.
        assertEquals(1, options.getUploadedFileHistory(msg2).size());
        assertEquals("file-2", options.getUploadedFileHistory(msg2).get(0).id());
    }
}
