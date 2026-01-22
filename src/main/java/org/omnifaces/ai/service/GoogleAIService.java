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
package org.omnifaces.ai.service;

import static org.omnifaces.ai.helper.ImageHelper.guessImageMediaType;
import static org.omnifaces.ai.helper.ImageHelper.toImageBase64;
import static org.omnifaces.ai.helper.TextHelper.isBlank;

import java.net.URI;
import java.util.List;

import jakarta.json.Json;

import org.omnifaces.ai.AIConfig;
import org.omnifaces.ai.AIModality;
import org.omnifaces.ai.AIProvider;
import org.omnifaces.ai.AIService;
import org.omnifaces.ai.model.ChatOptions;
import org.omnifaces.ai.model.GenerateImageOptions;

/**
 * AI service implementation using Google AI API.
 *
 * <h2>Required Configuration</h2>
 * <p>
 * The following configuration properties must be provided via {@link AIConfig}:
 * <ul>
 *     <li>provider: {@link AIProvider#GOOGLE}</li>
 *     <li>apiKey: your Google API key</li>
 * </ul>
 *
 * <h2>Optional Configuration</h2>
 * <p>
 * The following configuration properties are optional.
 * See {@link AIProvider#GOOGLE} for defaults.
 * <ul>
 *     <li>model: the model to use</li>
 *     <li>endpoint: the API endpoint URL</li>
 * </ul>
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see AIProvider#GOOGLE
 * @see AIConfig
 * @see BaseAIService
 * @see AIService
 * @see <a href="https://ai.google.dev/api">API Reference</a>
 */
public class GoogleAIService extends BaseAIService {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a Google AI service with the specified configuration.
     *
     * @param config the AI configuration
     * @see AIConfig
     */
    public GoogleAIService(AIConfig config) {
        super(config);
    }

    @Override
    public boolean supportsModality(AIModality modality) {
        var fullModelName = getModelName().toLowerCase();

        return switch (modality) {
            case IMAGE_ANALYSIS -> true;
            case IMAGE_GENERATION -> fullModelName.contains("image");
            default -> false;
        };
    }

    /**
     * Technically, Google AI API supports it, but not as SSE. It only supports JSON streaming. Chunks of a whole JSON object. Useless thus.
     */
    @Override
    protected boolean supportsStreaming() {
        return false;
    }

    @Override
    protected URI resolveURI(String path) {
        return super.resolveURI(String.format("models/%s:%s?key=%s", model, path, apiKey));
    }

    /**
     * Returns {@code generateContent}.
     */
    @Override
    protected String getChatPath(boolean streaming) {
        return "generateContent";
    }

    @Override
    protected String buildChatPayload(String message, ChatOptions options, boolean streaming) {
        if (isBlank(message)) {
            throw new IllegalArgumentException("Message cannot be blank");
        }

        var payload = Json.createObjectBuilder();

        if (!isBlank(options.getSystemPrompt())) {
            payload.add("system_instruction", Json.createObjectBuilder()
                .add("parts", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("text", options.getSystemPrompt()))));
        }

        payload.add("contents", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("text", message)))));

        var generationConfig = Json.createObjectBuilder()
            .add("temperature", options.getTemperature())
            .add("maxOutputTokens", options.getMaxTokens());

        if (options.getTopP() != 1.0) {
            generationConfig.add("topP", options.getTopP());
        }

        return payload
            .add("generationConfig", generationConfig)
            .build()
            .toString();
    }

    @Override
    protected String buildVisionPayload(byte[] image, String prompt) {
        if (isBlank(prompt)) {
            throw new IllegalArgumentException("Prompt cannot be blank");
        }

        var base64 = toImageBase64(image);
        return Json.createObjectBuilder()
            .add("contents", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("inline_data", Json.createObjectBuilder()
                                .add("mime_type", guessImageMediaType(base64))
                                .add("data", base64)))
                        .add(Json.createObjectBuilder()
                            .add("text", prompt)))))
            .build()
            .toString();
    }

    @Override
    protected String buildGenerateImagePayload(String prompt, GenerateImageOptions options) {
        if (isBlank(prompt)) {
            throw new IllegalArgumentException("Prompt cannot be blank");
        }

        var generationConfig = Json.createObjectBuilder()
            .add("responseModalities", Json.createArrayBuilder().add("IMAGE"))
            .add("imageConfig", Json.createObjectBuilder()
                .add("aspectRatio", options.getAspectRatio()));

        return Json.createObjectBuilder()
            .add("contents", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("text", prompt)))))
            .add("generationConfig", generationConfig)
            .build()
            .toString();
    }

    @Override
    protected List<String> getResponseMessageContentPaths() {
        return List.of("candidates[0].content.parts[0].text");
    }

    @Override
    protected List<String> getResponseImageContentPaths() {
        return List.of("candidates[0].content.parts[0].inlineData.data");
    }
}
