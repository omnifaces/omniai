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

/**
 * Provider-specific handlers for text and image modalities.
 * <p>
 * AI providers differ in their JSON payload formats and response structures. This package contains
 * {@link org.omnifaces.ai.AITextHandler} and {@link org.omnifaces.ai.AIImageHandler} implementations that adapt the
 * generic API to provider-specific requirements:
 * <ul>
 * <li>{@link org.omnifaces.ai.modality.BaseAITextHandler} / {@link org.omnifaces.ai.modality.BaseAIImageHandler} - sensible defaults for most LLMs</li>
 * <li>{@code OpenAITextHandler} / {@code OpenAIImageHandler} - OpenAI-specific handling</li>
 * <li>{@code AnthropicAITextHandler} / {@code AnthropicAIImageHandler} - Anthropic-specific handling</li>
 * <li>{@code GoogleAITextHandler} / {@code GoogleAIImageHandler} - Google AI-specific handling</li>
 * <li>... and others for xAI, Meta, Ollama, OpenRouter</li>
 * </ul>
 * Custom handlers can be specified via {@link org.omnifaces.ai.cdi.AI#textHandler()} and
 * {@link org.omnifaces.ai.cdi.AI#imageHandler()} to customize request payloads or response parsing.
 *
 * @see org.omnifaces.ai.AITextHandler
 * @see org.omnifaces.ai.AIImageHandler
 */
package org.omnifaces.ai.modality;
