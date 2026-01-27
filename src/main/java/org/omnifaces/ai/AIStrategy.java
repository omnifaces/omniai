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
package org.omnifaces.ai;

import java.io.Serializable;

/**
 * Strategy for AI services.
 *
 * @param textHandler The text handler, or {@code null} to use the AI provider's default.
 * @param imageHandler The image handler, or {@code null} to use the AI provider's default.
 * @see AIConfig
 * @see AIService
 * @see AITextHandler
 * @see AIImageHandler
 * @author Bauke Scholtz
 * @since 1.0
 */
public final record AIStrategy(Class<? extends AITextHandler> textHandler, Class<? extends AIImageHandler> imageHandler) implements Serializable {}
