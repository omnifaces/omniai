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
package org.omnifaces.ai.service;

import static java.net.http.HttpClient.newBuilder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.omnifaces.ai.exception.AIApiBadRequestException;
import org.omnifaces.ai.exception.AIApiException;
import org.omnifaces.ai.exception.AIException;

/**
 * API client utility for {@link BaseAIService} implementations.
 * <p>
 * This utility wraps {@link java.net.http.HttpClient} to provide a simple API interface for all {@link BaseAIService} implementations.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
final class AIApiClient {

    /** The User-Agent header: {@value} */
    public static final String USER_AGENT = "OmniAI 1.0 (https://github.com/omnifaces/omniai)";
    /** Default max retries: {@value} */
    public static final int MAX_RETRIES = 3;
    /** Initial retry backoff time: {@value}ms (increases exponentially on every retry) */
    public static final long INITIAL_BACKOFF_MS = 1000;

    private final HttpClient client;
    private final Duration requestTimeout;

    private AIApiClient(HttpClient client, Duration requestTimeout) {
        this.client = client;
        this.requestTimeout = requestTimeout;
    }

    /**
     * Creates a new API client instance with custom timeouts.
     *
     * @param connectTimeout The connection timeout
     * @param requestTimeout The request timeout
     * @return A new API client instance
     */
    public static AIApiClient newInstance(Duration connectTimeout, Duration requestTimeout) {
        return new AIApiClient(newBuilder().connectTimeout(connectTimeout).build(), requestTimeout);
    }

    /**
     * Sends a POST request for the specified {@link BaseAIService}.
     * Will retry at most {@value #MAX_RETRIES} times in case of a connection error with exponentially incremental backoff of {@value #INITIAL_BACKOFF_MS}ms.
     *
     * @param service The {@link BaseAIService} to extract URI and headers from.
     * @param path the API path
     * @param body The request body
     * @return The response body as a string
     * @throws AIApiException if the request fails
     */
    public CompletableFuture<String> post(BaseAIService service, String path, String body) throws AIApiException {
        return send(HttpRequest.newBuilder(service.resolveURI(path)).timeout(requestTimeout).POST(BodyPublishers.ofString(body)), service.getRequestHeaders());
    }

    private CompletableFuture<String> send(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
        requestBuilder.header("User-Agent", USER_AGENT);
        requestBuilder.header("Content-Type", "application/json");
        headers.forEach(requestBuilder::header);
        var request = requestBuilder.build();
        return attemptSendAsync(request, 0);
    }

    private CompletableFuture<String> attemptSendAsync(HttpRequest request, int attempt) {
        return client.sendAsync(request, BodyHandlers.ofString())
            .thenCompose(response -> {
                var statusCode = response.statusCode();

                if (statusCode >= AIApiBadRequestException.STATUS_CODE) {
                    return CompletableFuture.failedFuture(AIApiException.forStatusCode(statusCode, request.uri().toString().split("\\?", 2)[0] + ": "+  response.body()));
                }

                return CompletableFuture.completedFuture(response.body());
            })
            .exceptionallyCompose(throwable -> {
                var cause = (throwable instanceof CompletionException completionException) ? completionException.getCause() : throwable;

                if (cause instanceof AIException) {
                    return CompletableFuture.failedFuture(cause);
                }
                else if (attempt >= MAX_RETRIES - 1 || !isRetryable(cause)) {
                    return CompletableFuture.failedFuture(new AIApiException("Request failed (" + attempt + " retries)", cause, 0));
                }

                var delayed = CompletableFuture.delayedExecutor(INITIAL_BACKOFF_MS * (1L << attempt), TimeUnit.MILLISECONDS);
                return CompletableFuture.supplyAsync(() -> attemptSendAsync(request, attempt + 1), delayed).thenCompose(f -> f);
            });
    }

    /**
     * Determines whether a failed request should be retried based on the exception.
     * <p>
     * Retryable errors are transient connection issues indicated by an {@link IOException}
     * with a message containing "terminated", "reset", or "refused" anywhere in the cause chain.
     *
     * @param throwable The exception to check.
     * @return {@code true} if the error is transient and the request should be retried.
     */
    private static boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        return Stream.iterate(throwable, Objects::nonNull, Throwable::getCause)
            .filter(IOException.class::isInstance)
            .findFirst()
            .map(ioException ->
                Stream.iterate(ioException, Objects::nonNull, Throwable::getCause)
                      .map(Throwable::getMessage)
                      .filter(Objects::nonNull)
                      .map(String::toLowerCase)
                      .anyMatch(msg -> msg.contains("terminated") || msg.contains("reset") || msg.contains("refused"))
            )
            .orElse(false);
    }
}
