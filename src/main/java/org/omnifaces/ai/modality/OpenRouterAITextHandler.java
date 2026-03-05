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

import jakarta.json.JsonObject;

import org.omnifaces.ai.AIService;
import org.omnifaces.ai.model.ChatInput;
import org.omnifaces.ai.model.ChatOptions;
import org.omnifaces.ai.service.AIServiceWrapper;
import org.omnifaces.ai.service.OpenRouterAIService;

/**
 * Default text handler for OpenRouter AI service.
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see OpenRouterAIService
 */
public class OpenRouterAITextHandler extends OpenAITextHandler {

    private static final long serialVersionUID = 1L;

    /**
     * @see <a href="https://openrouter.ai/docs/guides/features/plugins/web-search">Web Search API Reference</a>
     */
    @Override
    public JsonObject buildChatPayload(AIService service, ChatInput input, ChatOptions options, boolean streaming) {
        if (options.useWebSearch()) {
            var newOptions = options.withWebSearch(null);

            if (options.getWebSearchLocation() != null) {
                newOptions = appendPrompt(options, "Search within " + options.getWebSearchLocation()); // OpenRouter API doesn't support user_location in payload.
            }

            return super.buildChatPayload(new AIServiceWrapper(service) {
                private static final long serialVersionUID = 1L;

                @Override
                public String getModelName() {
                    return super.getModelName() + ":online";
                }
            }, input, newOptions, streaming);
        }
        else {
            return super.buildChatPayload(service, input, options, streaming);
        }
    }
}
