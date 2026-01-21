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
 * Exception thrown when the AI API returns HTTP 429 Too Many Requests.
 * <p>
 * This indicates the rate limit has been exceeded. Consider implementing retry logic with exponential backoff, or reducing request frequency.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public class AIApiRateLimitExceededException extends AIApiException {

    private static final long serialVersionUID = 1L;

    /** The HTTP status code for Too Many Requests: {@value}. */
    public static final int STATUS_CODE = 429;

    /**
     * Constructs a new rate limit exceeded exception with the specified HTTP request URI and HTTP response body.
     *
     * @param uri The HTTP request URI.
     * @param responseBody The HTTP response body.
     */
    public AIApiRateLimitExceededException(URI uri, String responseBody) {
        super(uri, STATUS_CODE, responseBody);
    }
}
