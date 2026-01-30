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
 * CDI integration for dependency injection of AI services.
 * <p>
 * This package provides the {@link org.omnifaces.ai.cdi.AI} qualifier annotation for injecting configured
 * {@link org.omnifaces.ai.AIService} instances:
 * <pre>
 * &#64;Inject
 * &#64;AI(provider = ANTHROPIC, apiKey = "#{config.apiKey}")
 * private AIService ai;
 * </pre>
 * Expression syntax (EL-style <code>#{...}</code>,  <code>${...}</code> or MicroProfile Config-style <code>${config:...}</code>) is supported for
 * configuration values, allowing externalization of API keys and other sensitive settings.
 *
 * @see org.omnifaces.ai.cdi.AI
 */
package org.omnifaces.ai.cdi;
