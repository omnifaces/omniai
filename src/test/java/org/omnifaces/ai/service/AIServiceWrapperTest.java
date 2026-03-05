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

import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.omnifaces.ai.AIService;

class AIServiceWrapperTest {

    /**
     * Validates that AIServiceWrapper implements all non-default methods declared in AIService.
     * <p>
     * This test uses reflection to compare all non-default methods declared in the AIService
     * interface against the methods implemented in AIServiceWrapper. When new non-default
     * methods are added to AIService, this test will fail until AIServiceWrapper is updated
     * to implement them.
     */
    @Test
    public void testImplementsAllNonDefaultAIServiceMethods() {
        var wrapperMethods = getNonPrivateMethodSignatures(AIServiceWrapper.class);
        var requiredMethods = getNonDefaultInterfaceMethodSignatures(AIService.class);
        var missingMethods = new HashSet<>(requiredMethods);
        missingMethods.removeAll(wrapperMethods);

        if (!missingMethods.isEmpty()) {
            fail("AIServiceWrapper is missing an implementation for the following AIService methods:" + lineSeparator()
                    + missingMethods.stream()
                            .sorted()
                            .map(methodSignature -> "  - AIService#" + methodSignature)
                            .collect(joining(lineSeparator())));
        }
    }

    private static Set<String> getNonPrivateMethodSignatures(Class<?> clazz) {
        return stream(clazz.getDeclaredMethods())
                .filter(not(AIServiceWrapperTest::isPrivateMethod))
                .map(AIServiceWrapperTest::toSignature)
                .collect(toSet());
    }

    private static Set<String> getNonDefaultInterfaceMethodSignatures(Class<?> iface) {
        if (!iface.isInterface()) {
            throw new IllegalArgumentException();
        }

        return stream(iface.getMethods())
                .filter(not(Method::isDefault))
                .filter(not(AIServiceWrapperTest::isObjectMethod))
                .map(AIServiceWrapperTest::toSignature)
                .collect(toSet());
    }

    private static boolean isPrivateMethod(Method method) {
        return Modifier.isPrivate(method.getModifiers());
    }

    private static boolean isObjectMethod(Method method) {
        try {
            Object.class.getMethod(method.getName(), method.getParameterTypes());
            return true;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static String toSignature(Method method) {
        var signature = new StringBuilder();
        signature.append(method.getName());
        signature.append('(');
        signature.append(stream(method.getParameterTypes()).map(Class::getSimpleName).collect(joining(", ")));
        signature.append(')');
        return signature.toString();
    }
}
