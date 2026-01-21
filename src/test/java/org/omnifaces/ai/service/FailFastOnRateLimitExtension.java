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

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.omnifaces.ai.exception.AIApiRateLimitExceededException;
import org.opentest4j.TestAbortedException;

public class FailFastOnRateLimitExtension implements BeforeEachCallback, TestExecutionExceptionHandler, AfterTestExecutionCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FailFastOnRateLimitExtension.class);

    private static final String RATE_LIMIT_HIT = "rateLimitHit";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (getStore(context).getOrDefault(RATE_LIMIT_HIT, Boolean.class, false)) {
            throw new TestAbortedException("Rate limit was hit in a previous test in this class; skipping remaining tests in this class, we better retry later.");
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (throwable instanceof AIApiRateLimitExceededException) {
            getStore(context).put(RATE_LIMIT_HIT, true);
        }

        throw throwable;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        // NOOP.
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getParent().orElse(context).getStore(NAMESPACE);
    }
}
