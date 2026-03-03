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
package org.omnifaces.ai.modality;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.omnifaces.ai.helper.JsonHelper.addStrictAdditionalProperties;
import static org.omnifaces.ai.helper.JsonHelper.checkErrors;
import static org.omnifaces.ai.helper.JsonHelper.findAllByPath;
import static org.omnifaces.ai.helper.JsonHelper.findByPath;
import static org.omnifaces.ai.helper.TextHelper.isBlank;
import static org.omnifaces.ai.model.Sse.Event.Type.DATA;
import static org.omnifaces.ai.model.Sse.Event.Type.EVENT;

import java.util.List;
import java.util.function.Consumer;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.omnifaces.ai.AIModelVersion;
import org.omnifaces.ai.AIService;
import org.omnifaces.ai.exception.AIResponseException;
import org.omnifaces.ai.exception.AITokenLimitExceededException;
import org.omnifaces.ai.model.ChatInput;
import org.omnifaces.ai.model.ChatInput.Message.Role;
import org.omnifaces.ai.model.ChatOptions;
import org.omnifaces.ai.model.ChatUsage;
import org.omnifaces.ai.model.Sse.Event;
import org.omnifaces.ai.service.AnthropicAIService;

/**
 * Default text handler for Anthropic AI service.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see AnthropicAIService
 */
public class AnthropicAITextHandler extends DefaultAITextHandler {

    private static final long serialVersionUID = 1L;

    private static final AIModelVersion CLAUDE_3 = AIModelVersion.of("claude", 3);
    private static final AIModelVersion CLAUDE_4_6 = AIModelVersion.of("claude", 4, 6);

    private static final int DEFAULT_MAX_TOKENS_CLAUDE_3_0 = 4096;
    private static final int DEFAULT_MAX_TOKENS_CLAUDE_3_X = 8192;

    /**
     * @see <a href="https://platform.claude.com/docs/en/api/messages">API Reference</a>
     */
    @Override
    public JsonObject buildChatPayload(AIService service, ChatInput input, ChatOptions options, boolean streaming) {
        var payload = Json.createObjectBuilder()
                .add("model", service.getModelName())
                .add("max_tokens", ofNullable(options.getMaxTokens()).orElseGet(() -> service.getModelVersion().lte(CLAUDE_3) ? DEFAULT_MAX_TOKENS_CLAUDE_3_0 : DEFAULT_MAX_TOKENS_CLAUDE_3_X)); // Required!
        var messages = Json.createArrayBuilder();
        buildChatPayloadTools(service, payload, options);
        buildChatPayloadSystemPrompt(payload, options);
        buildChatPayloadHistoryMessages(messages, input);
        buildChatPayloadUserContent(messages, input, service, options);
        payload.add("messages", messages);
        buildChatPayloadGenerationConfig(payload, service, options, streaming);
        return payload.build();
    }

    /**
     * Add tools to the payload as a top-level {@code tools} field.
     * @param service The current AI service.
     * @param payload The payload builder.
     * @param options The chat options.
     * @since 1.3
     * @see <a href="https://platform.claude.com/docs/en/agents-and-tools/tool-use/web-search-tool">Web Search Tool Reference</a>
     */
    protected void buildChatPayloadTools(AIService service, JsonObjectBuilder payload, ChatOptions options) {
        if (options.isWebSearch()) {
            payload.add("tools", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("type", service.getModelVersion().gte(CLAUDE_4_6) ? "web_search_20260209" : "web_search_20250305")
                    .add("name", "web_search").build()));
        }
    }

    /**
     * Add system prompt to the payload as a top-level {@code system} field.
     * @param payload The payload builder.
     * @param options The chat options.
     */
    protected void buildChatPayloadSystemPrompt(JsonObjectBuilder payload, ChatOptions options) {
        if (!isBlank(options.getSystemPrompt())) {
            payload.add("system", options.getSystemPrompt());
        }
    }

    /**
     * Add conversation history messages to the messages array.
     * @param messages The messages array builder.
     * @param input The chat input.
     */
    protected void buildChatPayloadHistoryMessages(JsonArrayBuilder messages, ChatInput input) {
        for (var historyMessage : input.getHistory()) {
            messages.add(Json.createObjectBuilder()
                .add("role", historyMessage.role() == Role.USER ? "user" : "assistant")
                .add("content", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("type", "text")
                        .add("text", historyMessage.content()))));
        }
    }

    /**
     * Add user content (images, files, and text message) to the messages array.
     * @param messages The messages array builder.
     * @param input The chat input.
     * @param service The visiting AI service.
     * @param options The chat options.
     */
    protected void buildChatPayloadUserContent(JsonArrayBuilder messages, ChatInput input, AIService service, ChatOptions options) {
        var content = Json.createArrayBuilder();

        for (var image : input.getImages()) {
            content.add(Json.createObjectBuilder()
                .add("type", "image")
                .add("source", Json.createObjectBuilder()
                    .add("type", "base64")
                    .add("media_type", image.mimeType().value())
                    .add("data", image.toBase64())));
        }

        if (!input.getFiles().isEmpty()) {
            checkSupportsFileAttachments(service);

            for (var file : input.getFiles()) {
                var fileId = service.upload(file, options);

                content.add(Json.createObjectBuilder()
                    .add("type", "document")
                    .add("source", Json.createObjectBuilder()
                        .add("type", "file")
                        .add("file_id", fileId)));
            }
        }

        content.add(Json.createObjectBuilder()
            .add("type", "text")
            .add("text", input.getMessage()));

        messages.add(Json.createObjectBuilder()
            .add("role", "user")
            .add("content", content));
    }

    /**
     * Add generation config (streaming, temperature, topP, structured output) to the payload.
     * @param payload The payload builder.
     * @param service The visiting AI service.
     * @param options The chat options.
     * @param streaming Whether streaming is enabled.
     */
    protected void buildChatPayloadGenerationConfig(JsonObjectBuilder payload, AIService service, ChatOptions options, boolean streaming) {
        if (streaming) {
            checkSupportsStreaming(service);
            payload.add("stream", true);
        }

        if (options.getTemperature() != 0.7) {
            payload.add("temperature", options.getTemperature());
        }

        if (options.getTopP() != 1.0) {
            payload.add("top_p", options.getTopP());
        }

        if (options.getJsonSchema() != null) {
            checkSupportsStructuredOutput(service);
            payload.add("output_format", Json.createObjectBuilder()
                .add("type", "json_schema")
                .add("schema", addStrictAdditionalProperties(options.getJsonSchema())));
        }
    }

    @Override
    public List<String> getChatResponseContentPaths() {
        return List.of("content[*].text");
    }

    @Override
    public List<String> getChatUsageInputTokensPaths() {
        return List.of("usage.input_tokens");
    }

    @Override
    public List<String> getChatUsageOutputTokensPaths() {
        return List.of("usage.output_tokens");
    }

    /**
     * An override which joins potentially multiple chat response parts into a single string.
     */
    @Override
    public String parseChatResponse(JsonObject responseJson) throws AIResponseException {
        if ("server_tool_use".equals(findByPath(responseJson, "content[0].type").orElse(null))) {
            checkErrors(responseJson, getTextResponseErrorMessagePaths());
            var messageContentPaths = getChatResponseContentPaths();

            if (messageContentPaths.isEmpty()) {
                throw new IllegalStateException("getChatResponseContentPaths() may not return an empty list");
            }

            return findAllByPath(responseJson, getChatResponseContentPaths().get(0)).stream().collect(joining());
        }
        else {
            return super.parseChatResponse(responseJson);
        }
    }

    @Override
    public boolean processChatStreamEvent(AIService service, ChatOptions options, Event event, Consumer<String> onToken) {
        if (event.type() == EVENT) {
            if ("max_tokens".equals(event.value())) {
                throw new AITokenLimitExceededException();
            }

            return !"message_stop".equals(event.value()) && !"content_block_stop".equals(event.value());
        }
        else if (event.type() == DATA) {
            return tryParseEventDataJson(event.value(), json -> {
                var type = json.getString("type", null);

                if ("content_block_delta".equals(type)) {
                    var token = json.getJsonObject("delta").getString("text", "");

                    if (!token.isEmpty()) { // Do not use isBlank! Whitespace can be a valid token.
                        onToken.accept(token);
                    }
                }
                else if (!options.isDefault()) {
                    if ("message_start".equals(type)) {
                        options.recordUsage(parseChatUsage(json.getJsonObject("message")));
                    }
                    else if ("message_delta".equals(type)) {
                        findByPath(json, getChatUsageOutputTokensPaths().get(0)).ifPresent(outputTokens -> options.recordUsage(new ChatUsage(options.getLastUsage() != null ? options.getLastUsage().inputTokens() : -1, Integer.parseInt(outputTokens), -1)));
                    }
                }
                else if ("error".equals(type)) {
                    throw new AIResponseException("Error event returned", event.value());
                }

                return true;
            });
        }

        return true;
    }
}
