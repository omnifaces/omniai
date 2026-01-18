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

import java.util.concurrent.CompletionException;

/**
 * Base exception for all AI service-related errors.
 * <p>
 * This is the root of the OmniAI exception hierarchy:
 * <ul>
 * <li>{@link AIApiException} - HTTP-level errors (4xx/5xx status codes)
 * <li>{@link AIApiResponseException} - Response content errors (parsing, missing content)
 * </ul>
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see AIApiException
 * @see AIApiResponseException
 */
public class AIException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Unwraps an {@link AIException} from a {@link CompletionException} thrown by async operations.
     * <p>
     * If the cause is already an {@code AIException}, it is returned directly.
     * Otherwise, a new {@code AIException} wrapping the cause is created.
     *
     * @param completionException The completion exception from an async operation.
     * @return The unwrapped or newly created AI exception.
     */
    public static AIException asyncRequestFailed(CompletionException completionException) {
        var cause = completionException.getCause();
        var exception = cause instanceof AIException casted ? casted : new AIException("Async request failed", cause);
        exception.addSuppressed(new Exception("Async thread"));
        return exception;
    }

    /**
     * Constructs a new AI exception with the specified message.
     *
     * @param message The detail message.
     */
    public AIException(String message) {
        super(message);
    }

    /**
     * Constructs a new AI exception with the specified message and cause.
     *
     * @param message The detail message.
     * @param cause The cause of this exception.
     */
    public AIException(String message, Throwable cause) {
        super(message, cause);
    }
}
