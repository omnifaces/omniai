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
package org.omnifaces.ai.exception;

/**
 * Exception thrown when an AI API request fails with an HTTP error status code.
 * <p>
 * This exception and its subclasses represent HTTP-level errors (4xx/5xx):
 * <ul>
 * <li>{@link AIBadRequestException} - 400 Bad Request
 * <li>{@link AIAuthenticationException} - 401 Unauthorized
 * <li>{@link AIAuthorizationException} - 403 Forbidden
 * <li>{@link AIEndpointNotFoundException} - 404 Not Found
 * <li>{@link AIRateLimitExceededException} - 429 Too Many Requests
 * <li>{@link AIServiceUnavailableException} - 503 Service Unavailable
 * </ul>
 * <p>
 * Use {@link #forStatusCode(int, String)} to create the appropriate subclass based on HTTP status code.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see AIApiResponseException
 */
public class AIApiException extends AIException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    /**
     * Creates the appropriate exception subclass based on the HTTP status code.
     *
     * @param statusCode The HTTP status code.
     * @param responseBody The response body containing error details.
     * @return The appropriate exception subclass, or a generic {@code AIApiException} for unmapped status codes.
     */
    public static AIApiException forStatusCode(int statusCode, String message) {
        return switch (statusCode) {
            case AIBadRequestException.STATUS_CODE -> new AIBadRequestException("API bad request: " + message);
            case AIAuthenticationException.STATUS_CODE -> new AIAuthenticationException("API authentication failed: " + message);
            case AIAuthorizationException.STATUS_CODE -> new AIAuthorizationException("API authorization failed: " + message);
            case AIEndpointNotFoundException.STATUS_CODE -> new AIEndpointNotFoundException("API endpoint not found: " + message);
            case AIRateLimitExceededException.STATUS_CODE -> new AIRateLimitExceededException("API rate limit exceeded: " + message);
            case AIServiceUnavailableException.STATUS_CODE -> new AIServiceUnavailableException("API service unavailable: " + message);
            default -> new AIApiException("API error " + statusCode + ": " + message, statusCode);
        };
    }

    /**
     * Constructs a new API exception with the specified message and status code.
     *
     * @param message The detail message.
     * @param statusCode The HTTP status code.
     */
    public AIApiException(String message, int statusCode) {
        this(message, null, statusCode);
    }

    /**
     * Constructs a new API exception with the specified message, cause, and status code.
     *
     * @param message The detail message.
     * @param cause The cause of this exception.
     * @param statusCode The HTTP status code.
     */
    public AIApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code.
     * @return The HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
