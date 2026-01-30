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
 * Exception hierarchy for AI service operations.
 * <p>
 * All exceptions extend {@link org.omnifaces.ai.exception.AIException}:
 * <ul>
 * <li>{@link org.omnifaces.ai.exception.AIHttpException} - HTTP-level errors (4xx/5xx responses)</li>
 * <li>{@link org.omnifaces.ai.exception.AIResponseException} - response parsing or content errors</li>
 * <li>{@link org.omnifaces.ai.exception.AITokenLimitExceededException} - input/output token limit exceeded</li>
 * </ul>
 * HTTP exceptions are further specialized for common error conditions (authentication, authorization, rate limiting,
 * bad request, endpoint not found, service unavailable).
 */
package org.omnifaces.ai.exception;
