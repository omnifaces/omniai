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

import java.net.URI;

/**
 * Exception thrown when an AI API request fails with an HTTP error status code.
 * <p>
 * This exception and its subclasses represent HTTP-level errors (4xx/5xx):
 * <ul>
 * <li>{@link AIApiBadRequestException} - 400 Bad Request
 * <li>{@link AIApiAuthenticationException} - 401 Unauthorized
 * <li>{@link AIApiAuthorizationException} - 403 Forbidden
 * <li>{@link AIApiEndpointNotFoundException} - 404 Not Found
 * <li>{@link AIApiRateLimitExceededException} - 429 Too Many Requests
 * <li>{@link AIApiServiceUnavailableException} - 503 Service Unavailable
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

    /** The HTTP request URI. */
    private final URI uri;

    /** The HTTP status code. */
    private final int statusCode;

    /** The HTTP response body. */
    private final String responseBody;

    /**
     * Creates and returns the most specific {@link AIApiException} subclass that matches the given HTTP status code.
     * <p>
     * If no specific exception type is defined for the status code, a generic {@link AIApiException} is returned.
     * The created exception includes the request URI, status code, and response body (when available) to help with debugging.
     *
     * @param uri The URI of the HTTP request that caused the error (used in exception messages).
     * @param statusCode The HTTP status code returned by the server.
     * @param responseBody The response body (may be {@code null} or empty).
     * @return a subclass of {@link AIApiException} matching the status code, or a generic {@link AIApiException}.
     */
    public static AIApiException fromStatusCode(URI uri, int statusCode, String responseBody) {
        return switch (statusCode) {
            case AIApiBadRequestException.STATUS_CODE -> new AIApiBadRequestException(uri, responseBody);
            case AIApiAuthenticationException.STATUS_CODE -> new AIApiAuthenticationException(uri, responseBody);
            case AIApiAuthorizationException.STATUS_CODE -> new AIApiAuthorizationException(uri, responseBody);
            case AIApiEndpointNotFoundException.STATUS_CODE -> new AIApiEndpointNotFoundException(uri, responseBody);
            case AIApiRateLimitExceededException.STATUS_CODE -> new AIApiRateLimitExceededException(uri, responseBody);
            case AIApiServiceUnavailableException.STATUS_CODE -> new AIApiServiceUnavailableException(uri, responseBody);
            default -> new AIApiException(uri, statusCode, responseBody);
        };
    }

    /**
     * Constructs a new API exception with the specified URI, status code, and response body.
     *
     * @param uri The HTTP request URI.
     * @param statusCode The HTTP status code.
     * @param responseBody The HTTP response body.
     */
    public AIApiException(URI uri, int statusCode, String responseBody) {
        super("HTTP " + statusCode + " at " + URI.create(uri.toString().split("\\?", 2)[0]) + ": " + responseBody);
        this.uri = uri;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Constructs a new API exception with the specified message, and cause. Use this when request threw an exception instead of returning a response.
     *
     * @param message The detail message.
     * @param cause The cause of this exception.
     */
    public AIApiException(String message, Throwable cause) {
        super(message, cause);
        this.uri = null;
        this.statusCode = 0;
        this.responseBody = null;
    }

    /**
     * returns The HTTP request URI.
     * @return The HTTP request URI.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns the HTTP status code.
     * @return The HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns The HTTP response body.
     * @return The HTTP response body.
     */
    public String getResponseBody() {
        return responseBody;
    }
}
