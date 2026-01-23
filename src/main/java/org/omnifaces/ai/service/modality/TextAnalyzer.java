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
import org.omnifaces.ai.exception.AIResponseException;
import org.omnifaces.ai.model.ModerationOptions;
import org.omnifaces.ai.model.ModerationResult;

/**
 * Contract for prompt construction, token estimation and result parsing for text analysis features of an {@link AIService}.
 * <p>
 * Covers:
 * <ul>
 * <li>summarization</li>
 * <li>key-point extraction</li>
 * <li>translation</li>
 * <li>language detection</li>
 * <li>content moderation</li>
 * </ul>
 * <p>
 * Creative / interpretive tasks use a configurable temperature ({@link #getDefaultCreativeTemperature()});
 * classification tasks use low fixed temperature.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see AIService
 * @see DefaultTextAnalyzer
 */
public interface TextAnalyzer {

    /**
     * Returns the default temperature used for creative or interpretative text analysis related operations in
     * {@link AIService}. These operations typically use a moderate temperature (e.g. 0.3-0.7) to allow natural
     * phrasing and reasoning.
     * <ul>
     * <li>summarization</li>
     * <li>key-point extraction</li>
     * </ul>
     * <p>
     * Deterministic / classification-style operations use by design a fixed low temperature (typically 0.0-0.1) to
     * ensure consistent, factual and reproducible results:
     * <ul>
     * <li>language detection</li>
     * <li>content moderation</li>
     * <li>translation</li>
     * </ul>
     * <p>
     * The default implementation {@link DefaultTextAnalyzer} returns 0.5.
     *
     * @return default temperature value in range 0.0-1.0 for summarization and key-point extraction.
     * @see AIService#summarize(String, int)
     * @see AIService#extractKeyPoints(String, int)
     */
    double getDefaultCreativeTemperature();

    /**
     * Builds the system prompt for {@link AIService#summarize(String, int)} and {@link AIService#summarizeAsync(String, int)}.
     *
     * @param maxWords Maximum words in summary.
     * @return The system prompt.
     */
    String buildSummarizePrompt(int maxWords);

    /**
     * Estimates the maximum number of tokens for {@link AIService#summarize(String, int)} and {@link AIService#summarizeAsync(String, int)}.
     *
     * @param maxWords Maximum words in summary.
     * @param estimatedMaxTokensPerWord Estimated max tokens per word. You can use the value from {@link AIService#getEstimatedTokensPerWord()}.
     * @return Estimated maximum number of tokens.
     */
    int estimateSummarizeMaxTokens(int maxWords, double estimatedMaxTokensPerWord);

    /**
     * Builds the system prompt for {@link AIService#extractKeyPoints(String, int)} and {@link AIService#extractKeyPointsAsync(String, int)}.
     *
     * @param maxPoints Maximum number of key points.
     * @return The system prompt.
     */
    String buildExtractKeyPointsPrompt(int maxPoints);

    /**
     * Estimates the maximum number of tokens for {@link AIService#extractKeyPoints(String, int)} and {@link AIService#extractKeyPointsAsync(String, int)}.
     *
     * @param maxPoints Maximum number of key points.
     * @param estimatedMaxTokensPerWord Estimated max tokens per word. You can use the value from {@link AIService#getEstimatedTokensPerWord()}.
     * @return Estimated maximum number of tokens.
     */
    int estimateExtractKeyPointsMaxTokens(int maxPoints, double estimatedMaxTokensPerWord);

    /**
     * Builds the system prompt for {@link AIService#translate(String, String, String)} and {@link AIService#translateAsync(String, String, String)}.
     *
     * @param sourceLang Source language ISO 639-1 code, or {@code null} for auto-detection.
     * @param targetLang Target language ISO 639-1 code.
     * @return The system prompt.
     */
    String buildTranslatePrompt(String sourceLang, String targetLang);

    /**
     * Estimates the maximum number of tokens for {@link AIService#translate(String, String, String)} and {@link AIService#translateAsync(String, String, String)}.
     *
     * @param text The text to translate.
     * @param estimatedMaxTokensPerWord Estimated max tokens per word. You can use the value from {@link AIService#getEstimatedTokensPerWord()}.
     * @return Estimated maximum number of tokens.
     */
    int estimateTranslateMaxTokens(String text, double estimatedMaxTokensPerWord);

    /**
     * Builds the system prompt for {@link AIService#detectLanguage(String)} and {@link AIService#detectLanguageAsync(String)}.
     *
     * @return The system prompt.
     */
    String buildDetectLanguagePrompt();

    /**
     * Estimates the maximum number of tokens for {@link AIService#detectLanguage(String)} and {@link AIService#detectLanguageAsync(String)}.
     *
     * @param estimatedMaxTokensPerWord Estimated max tokens per word. You can use the value from {@link AIService#getEstimatedTokensPerWord()}.
     * @return Estimated maximum number of tokens.
     */
    int estimateDetectLanguageMaxTokens(double estimatedMaxTokensPerWord);


    /**
     * Builds the system prompt for {@link AIService#moderateContent(String, ModerationOptions)} and {@link AIService#moderateContentAsync(String, ModerationOptions)}.
     *
     * @param options Moderation options containing categories and threshold.
     * @return The system prompt.
     */
    String buildModerateContentPrompt(ModerationOptions options);

    /**
     * Estimates the maximum number of tokens for {@link AIService#moderateContent(String, ModerationOptions)} and {@link AIService#moderateContentAsync(String, ModerationOptions)}.
     *
     * @param options Moderation options containing categories and threshold.
     * @param estimatedMaxTokensPerWord Estimated max tokens per word. You can use the value from {@link AIService#getEstimatedTokensPerWord()}.
     * @return Estimated maximum number of tokens.
     */
    int estimateModerateContentMaxTokens(ModerationOptions options, double estimatedMaxTokensPerWord);

    /**
     * Parses the moderation result from response returned by {@link AIService#moderateContent(String, ModerationOptions)} and {@link AIService#moderateContentAsync(String, ModerationOptions)}.
     *
     * @param responseBody The response from the AI model containing moderation scores.
     * @param options The moderation options containing categories and threshold.
     * @return The parsed moderation result.
     * @throws AIResponseException If the response cannot be parsed.
     */
    ModerationResult parseModerationResult(String responseBody, ModerationOptions options) throws AIResponseException;
}
