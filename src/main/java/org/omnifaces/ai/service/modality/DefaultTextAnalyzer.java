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

import static org.omnifaces.ai.helper.JsonHelper.parseJson;

import java.util.TreeMap;

import jakarta.json.Json;

import org.omnifaces.ai.AIService;
import org.omnifaces.ai.exception.AIApiResponseException;
import org.omnifaces.ai.model.ModerationOptions;
import org.omnifaces.ai.model.ModerationResult;

/**
 * Default implementation of {@link TextAnalyzer} that provides sensible, general-purpose prompt templates, token
 * estimation logic, and response parsing suitable for most modern large language models (LLMs).
 * <p>
 * This class is intended as a reasonable fallback / starting point when no provider-specific implementation is
 * available or desired. It uses widely compatible prompt patterns that perform acceptably on models from OpenAI,
 * Anthropic, Google, xAI, Meta, Mistral, and similar instruction-tuned LLMs.
 *
 * <h2>When to extend or override</h2>
 * <p>
 * Subclass and override individual methods when you need to:
 * <ul>
 * <li>adapt prompts to a specific model's preferred style / few-shot examples</li>
 * <li>use model-family-specific tokenizers for more accurate estimation</li>
 * <li>support non-JSON moderation formats</li>
 * <li>change default temperature, output formatting rules, or safety instructions</li>
 * </ul>
 * <p>
 * This implementation makes no provider-specific API calls or assumptions, it only generates default prompts and
 * parses default moderation response.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see TextAnalyzer
 * @see AIService
 */
public class DefaultTextAnalyzer implements TextAnalyzer {

    /** Default reasoning tokens: {@value} */
    protected static final int DEFAULT_REASONING_TOKENS = 500;
    /** Default detection tokens: {@value} */
    protected static final int DEFAULT_DETECTION_TOKENS = 100;
    /** Default max words per keypoint: {@value} */
    protected static final int DEFAULT_MAX_WORDS_PER_KEYPOINT = 25;
    /** Default words per moderate content category: {@value} */
    protected static final int DEFAULT_WORDS_PER_MODERATE_CONTENT_CATEGORY = 10;
    /** Default text analysis temperature: {@value} */
    protected static final double DEFAULT_TEXT_ANALYSIS_TEMPERATURE = 0.5;

    @Override
    public double getDefaultCreativeTemperature() {
        return DEFAULT_TEXT_ANALYSIS_TEMPERATURE;
    }

    @Override
    public String buildSummarizePrompt(int maxWords) {
        return """
            You are a professional summarizer.
            Summarize the provided text in at most %d words.
            Rules:
            - Provide one coherent summary.
            Output format:
            - Plain text summary only.
            - No explanations, no notes, no extra text, no markdown formatting.
        """.formatted(maxWords);
    }

    @Override
    public int estimateSummarizeMaxTokens(int maxWords, double estimatedTokensPerWord) {
        return DEFAULT_REASONING_TOKENS + (int) Math.ceil(maxWords * estimatedTokensPerWord);
    }

    @Override
    public String buildExtractKeyPointsPrompt(int maxPoints) {
        return """
            You are an expert at extracting key points.
            Extract the %d most important key points from the provided text.
            Each key point can have at most %d words.
            Output format:
            - One key point per line.
            - No numbering, no bullets, no dashes, no explanations, no notes, no extra text, no markdown formatting.
        """.formatted(maxPoints, DEFAULT_MAX_WORDS_PER_KEYPOINT);
    }

    @Override
    public int estimateExtractKeyPointsMaxTokens(int maxPoints, double estimatedTokensPerWord) {
        return DEFAULT_REASONING_TOKENS + (int) Math.ceil(maxPoints * DEFAULT_MAX_WORDS_PER_KEYPOINT * estimatedTokensPerWord);
    }

    @Override
    public String buildTranslatePrompt(String sourceLang, String targetLang) {
        var sourcePrompt = sourceLang == null
                ? "Detect the source language automatically."
                : "Translate from ISO 639-1 code '%s'".formatted(sourceLang.toLowerCase());
        return """
            You are a professional translator.
            %s
            Translate to ISO 639-1 code '%s'.
            Rules for every input:
            - Preserve ALL placeholders (#{...}, ${...}, {{...}}, etc) EXACTLY as-is.
            Rules if the input is parseable as HTML/XML:
            - Preserve ALL <script> tags (<script>...</script>) EXACTLY as-is.
            - Preserve ALL HTML/XML attribute values (style="...", class="...", id="...", data-*, etc.) EXACTLY as-is.
            Output format:
            - Only the translated input.
            - No explanations, no notes, no extra text, no markdown formatting.
            - Keep exact same line breaks, spacing and structure where possible.
        """.formatted(sourcePrompt, targetLang.toLowerCase());
    }

    @Override
    public int estimateTranslateMaxTokens(String text, double estimatedTokensPerWord) {
        return DEFAULT_REASONING_TOKENS + (int) Math.ceil(text.split("\\s+").length * estimatedTokensPerWord);
    }

    @Override
    public String buildDetectLanguagePrompt() {
        return """
            You are a language detection expert.
            Determine the language of the provided text.
            Output format:
            - Only the ISO 639-1 two-letter code of the main language (e.g. en, fr, es, zh).
            - No explanations, no notes, no extra text, no markdown formatting.
        """;
    }

    @Override
    public int estimateDetectLanguageMaxTokens(double estimatedTokensPerWord) {
        return DEFAULT_DETECTION_TOKENS + (int) Math.ceil(2 * estimatedTokensPerWord);
    }

    @Override
    public String buildModerateContentPrompt(ModerationOptions options) {
        var scores = Json.createObjectBuilder();
        options.getCategories().forEach(category -> scores.add(category, 0.0));
        var scoresTemplateString = Json.createObjectBuilder().add("scores", scores).build().toString();
        return """
            You are a strict content moderation model whose only task is to evaluate safety violations.
            Categories to evaluate:
            %s
            Rules:
            - 0.0: no violation at all
            - 0.1 - 0.3: mild / subtle undertone / possible concern
            - 0.4 - 0.6: moderate / clear but not extreme
            - 0.7 - 0.9: strong violation
            - 1.0: extremely severe / blatant / dangerous content
            Think carefully step by step:
            1. Read the whole message
            2. For each category, decide whether it applies
            3. Assign a score using the scale above
            4. Be objective; do not over-react to fictional, humorous, historical, or artistic context unless it clearly promotes harm
            JSON template:
            %s
            Output format:
            - Return ONLY valid JSON using the JSON template with scores substituted.
            - No explanations, no notes, no extra text, no markdown formatting.
        """.formatted(String.join(", ", options.getCategories()), scoresTemplateString);
    }

    @Override
    public int estimateModerateContentMaxTokens(ModerationOptions options, double estimatedTokensPerWord) {
        return DEFAULT_REASONING_TOKENS + (int) Math.ceil(options.getCategories().size() * DEFAULT_WORDS_PER_MODERATE_CONTENT_CATEGORY * estimatedTokensPerWord);
    }


    @Override
    public ModerationResult parseModerationResult(String responseBody, ModerationOptions options) throws AIApiResponseException {
        var responseJson = parseJson(responseBody);
        var scores = new TreeMap<String, Double>();
        var flagged = false;

        if (responseJson.containsKey("scores")) {
            var categoryScores = responseJson.getJsonObject("scores");
            for (String category : options.getCategories()) {
                if (categoryScores.containsKey(category)) {
                    double score = categoryScores.getJsonNumber(category).doubleValue();
                    scores.put(category, score);
                    if (score > options.getThreshold()) {
                        flagged = true;
                    }
                }
            }
        }

        return new ModerationResult(flagged, scores);
    }
}
