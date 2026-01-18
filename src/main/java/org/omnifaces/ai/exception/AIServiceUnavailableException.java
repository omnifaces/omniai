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
 * Exception thrown when the AI API returns HTTP 503 Service Unavailable.
 * <p>
 * This indicates the AI service is temporarily unavailable, typically due to high load or maintenance. Retry the request after a short delay.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public class AIServiceUnavailableException extends AIApiException {

    private static final long serialVersionUID = 1L;

    /** The HTTP status code for Service Unavailable: {@value}. */
    public static final int STATUS_CODE = 503;

    /**
     * Constructs a new service unavailable exception with the specified message.
     *
     * @param message The detail message.
     */
    public AIServiceUnavailableException(String message) {
        super(message, STATUS_CODE);
    }
}
