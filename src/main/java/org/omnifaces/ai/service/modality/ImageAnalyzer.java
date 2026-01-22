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
package org.omnifaces.ai.service.modality;

import org.omnifaces.ai.AIService;

/**
 * Contract for prompt construction for image analysis / vision features of an {@link AIService}.
 * <p>
 * Covers:
 * <ul>
 * <li>detailed image analysis / description / VQA</li>
 * <li>alt-text generation</li>
 * </ul>
 * <p>
 * No temperature control is used. Most vision models produce deterministic or near-deterministic output.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see AIService
 * @see DefaultImageAnalyzer
 */
public interface ImageAnalyzer {

    /**
     * Builds the default system prompt to use when no custom user prompt is provided to
     * {@link AIService#analyzeImage(byte[], String)} or {@link AIService#analyzeImageAsync(byte[], String)}.
     *
     * @return The general-purpose image analysis prompt.
     */
    String buildAnalyzeImagePrompt();

    /**
     * Builds the system prompt for {@link AIService#generateAltText(byte[])} and {@link AIService#generateAltTextAsync(byte[])}.
     *
     * @return The system prompt.
     */
    String buildGenerateAltTextPrompt();
}
