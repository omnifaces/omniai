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
package org.omnifaces.ai.helper;

import static java.lang.String.format;
import static org.omnifaces.ai.helper.JsonHelper.parseJson;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Utility class for generating JSON schemas from Java records and beans, and parsing JSON back into typed objects.
 * <p>
 * This class provides bidirectional conversion between Java types and JSON:
 * <ul>
 * <li>{@link #buildJsonSchema(Class)} - generates a JSON schema from a Java type</li>
 * <li>{@link #fromJson(String, Class)} - parses JSON into a typed Java object</li>
 * </ul>
 * <p>
 * Supported types:
 * <ul>
 * <li>Records (via {@link Class#getRecordComponents()})</li>
 * <li>Beans (via {@link Introspector})</li>
 * <li>Primitive types and their wrappers</li>
 * <li>Strings, enums, and common numeric types</li>
 * <li>Collections and arrays</li>
 * <li>Maps (with {@code additionalProperties} for value type)</li>
 * <li>Nested complex types (recursive)</li>
 * <li>{@link Optional} fields (excluded from "required", parsed as {@code Optional.empty()} when null)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * record ProductReview(String sentiment, int rating, List&lt;String&gt; pros, List&lt;String&gt; cons) {}
 *
 * // Generate schema for AI structured output
 * JsonObject schema = JsonSchemaHelper.buildJsonSchema(ProductReview.class);
 *
 * // Parse AI response back to typed object
 * ProductReview review = JsonSchemaHelper.fromJson(responseJson, ProductReview.class);
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public final class JsonSchemaHelper {

    private static final String ERROR_INVALID_BEAN = "Cannot introspect type '%s' as bean.";

    private JsonSchemaHelper() {
        throw new AssertionError();
    }

    /**
     * Builds a JSON schema for the given type.
     *
     * @param type The Java class to generate a JSON schema for.
     * @return The JSON schema as a {@link JsonObject}.
     */
    public static JsonObject buildJsonSchema(Class<?> type) {
        return buildObjectSchema(type, new HashSet<>());
    }

    private static JsonObject buildObjectSchema(Class<?> clazz, Set<Class<?>> visited) {
        if (visited.contains(clazz)) {
            return Json.createObjectBuilder().add("type", "object").build();
        }

        visited.add(clazz);

        var properties = Json.createObjectBuilder();
        var required = Json.createArrayBuilder();

        if (clazz.isRecord()) {
            for (var component : clazz.getRecordComponents()) {
                addRecordComponent(component, properties, required, visited);
            }
        }
        else {
            addBeanProperties(clazz, properties, required, visited);
        }

        return Json.createObjectBuilder()
            .add("type", "object")
            .add("properties", properties)
            .add("required", required)
            .build();
    }

    private static void addRecordComponent(RecordComponent component, JsonObjectBuilder properties, JsonArrayBuilder required, Set<Class<?>> visited) {
        var name = component.getName();
        var genericType = component.getGenericType();
        var rawType = component.getType();

        properties.add(name, buildTypeSchema(genericType, rawType, visited));

        if (!isOptional(rawType)) {
            required.add(name);
        }
    }

    private static void addBeanProperties(Class<?> clazz, JsonObjectBuilder properties, JsonArrayBuilder required, Set<Class<?>> visited) {
        PropertyDescriptor[] descriptors;

        try {
            descriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException(format(ERROR_INVALID_BEAN, clazz), e);
        }

        for (var descriptor : descriptors) {
            if (descriptor.getReadMethod() == null || "class".equals(descriptor.getName())) {
                continue;
            }

            var name = descriptor.getName();
            var genericType = descriptor.getReadMethod().getGenericReturnType();
            var rawType = descriptor.getPropertyType();

            properties.add(name, buildTypeSchema(genericType, rawType, visited));

            if (!isOptional(rawType)) {
                required.add(name);
            }
        }
    }

    private static JsonObjectBuilder buildTypeSchema(Type genericType, Class<?> rawType, Set<Class<?>> visited) {
        if (isOptional(rawType) && genericType instanceof ParameterizedType parameterized) {
            var typeArg = parameterized.getActualTypeArguments()[0];
            var innerRaw = typeArg instanceof Class<?> c ? c : Object.class;
            return buildTypeSchema(typeArg, innerRaw, visited);
        }

        if (rawType == String.class || rawType == char.class || rawType == Character.class) {
            return Json.createObjectBuilder().add("type", "string");
        }

        if (rawType == boolean.class || rawType == Boolean.class) {
            return Json.createObjectBuilder().add("type", "boolean");
        }

        if (rawType == int.class || rawType == Integer.class ||
            rawType == long.class || rawType == Long.class ||
            rawType == short.class || rawType == Short.class ||
            rawType == byte.class || rawType == Byte.class ||
            rawType == BigInteger.class) {
            return Json.createObjectBuilder().add("type", "integer");
        }

        if (rawType == double.class || rawType == Double.class ||
            rawType == float.class || rawType == Float.class ||
            rawType == BigDecimal.class) {
            return Json.createObjectBuilder().add("type", "number");
        }

        if (rawType.isEnum()) {
            return buildEnumSchema(rawType);
        }

        if (rawType.isArray()) {
            return buildArraySchema(rawType.getComponentType(), rawType.getComponentType(), visited);
        }

        if (Collection.class.isAssignableFrom(rawType)) {
            return buildCollectionSchema(genericType, visited);
        }

        if (Map.class.isAssignableFrom(rawType)) {
            return buildMapSchema(genericType, visited);
        }

        var nestedSchema = buildObjectSchema(rawType, visited);
        return Json.createObjectBuilder()
            .add("type", "object")
            .add("properties", nestedSchema.getJsonObject("properties"))
            .add("required", nestedSchema.getJsonArray("required"));
    }

    private static JsonObjectBuilder buildEnumSchema(Class<?> enumType) {
        var enumValues = Json.createArrayBuilder();

        for (var constant : enumType.getEnumConstants()) {
            enumValues.add(((Enum<?>) constant).name());
        }

        return Json.createObjectBuilder()
            .add("type", "string")
            .add("enum", enumValues);
    }

    private static JsonObjectBuilder buildArraySchema(Type elementType, Class<?> elementRawType, Set<Class<?>> visited) {
        return Json.createObjectBuilder()
            .add("type", "array")
            .add("items", buildTypeSchema(elementType, elementRawType, visited));
    }

    private static JsonObjectBuilder buildCollectionSchema(Type genericType, Set<Class<?>> visited) {
        if (genericType instanceof ParameterizedType parameterized) {
            var typeArgs = parameterized.getActualTypeArguments();

            if (typeArgs.length > 0) {
                var elementType = typeArgs[0];
                var elementRawType = elementType instanceof Class<?> c ? c : Object.class;
                return buildArraySchema(elementType, elementRawType, visited);
            }
        }

        return Json.createObjectBuilder()
            .add("type", "array")
            .add("items", Json.createObjectBuilder().add("type", "object"));
    }

    private static JsonObjectBuilder buildMapSchema(Type genericType, Set<Class<?>> visited) {
        if (genericType instanceof ParameterizedType parameterized) {
            var typeArgs = parameterized.getActualTypeArguments();

            if (typeArgs.length > 1) {
                var valueType = typeArgs[1];
                var valueRawType = valueType instanceof Class<?> c ? c : Object.class;
                return Json.createObjectBuilder()
                    .add("type", "object")
                    .add("additionalProperties", buildTypeSchema(valueType, valueRawType, visited));
            }
        }

        return Json.createObjectBuilder().add("type", "object");
    }

    private static boolean isOptional(Class<?> type) {
        return type == Optional.class;
    }


    // JSON to Object Parsing ------------------------------------------------------------------------------------------

    private static final String ERROR_PARSE_RECORD = "Cannot instantiate record '%s'.";
    private static final String ERROR_PARSE_BEAN = "Cannot instantiate bean '%s'.";
    private static final String ERROR_SET_PROPERTY = "Cannot set property '%s' on bean '%s'.";

    /**
     * Parses a JSON string into an instance of the specified type.
     * <p>
     * This method is the inverse of {@link #buildJsonSchema(Class)} - it converts JSON that conforms to the
     * generated schema back into a Java object. It supports:
     * <ul>
     * <li>Records (via canonical constructor)</li>
     * <li>Beans (via no-arg constructor and setters)</li>
     * <li>Primitive types and their wrappers</li>
     * <li>Strings, enums, and common numeric types</li>
     * <li>Collections ({@link List}, {@link Set})</li>
     * <li>Maps</li>
     * <li>Nested complex types</li>
     * <li>{@link Optional} fields (null or missing → {@code Optional.empty()})</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * record ProductReview(String sentiment, int rating, List&lt;String&gt; pros, List&lt;String&gt; cons) {}
     *
     * String json = "{\"sentiment\":\"positive\",\"rating\":5,\"pros\":[\"great\"],\"cons\":[]}";
     * ProductReview review = JsonSchemaHelper.fromJson(json, ProductReview.class);
     * </pre>
     *
     * @param <T> The target type.
     * @param json The JSON string to parse.
     * @param type The target class.
     * @return An instance of the target type populated from the JSON.
     * @throws IllegalArgumentException If the JSON cannot be parsed or the type cannot be instantiated.
     */
    public static <T> T fromJson(String json, Class<T> type) {
        var jsonObject = parseJson(json);
        return parseObject(jsonObject, type);
    }

    private static <T> T parseObject(JsonObject jsonObject, Class<T> rawType) {
        if (rawType.isRecord()) {
            return parseRecord(jsonObject, rawType);
        }
        else {
            return parseBean(jsonObject, rawType);
        }
    }

    private static <T> T parseRecord(JsonObject jsonObject, Class<T> recordClass) {
        var components = recordClass.getRecordComponents();
        var paramTypes = new Class<?>[components.length];
        var args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            paramTypes[i] = component.getType();
            var jsonValue = jsonObject.get(component.getName());
            args[i] = parseValue(jsonValue, component.getType(), component.getGenericType());
        }

        try {
            var constructor = recordClass.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(args);
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(format(ERROR_PARSE_RECORD, recordClass), e);
        }
    }

    private static <T> T parseBean(JsonObject jsonObject, Class<T> beanClass) {
        T instance;

        try {
            instance = beanClass.getDeclaredConstructor().newInstance();
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(format(ERROR_PARSE_BEAN, beanClass), e);
        }

        PropertyDescriptor[] descriptors;

        try {
            descriptors = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
        }
        catch (IntrospectionException e) {
            throw new IllegalArgumentException(format(ERROR_INVALID_BEAN, beanClass), e);
        }

        for (var descriptor : descriptors) {
            if (descriptor.getWriteMethod() == null || "class".equals(descriptor.getName())) {
                continue;
            }

            var jsonValue = jsonObject.get(descriptor.getName());

            if (jsonValue != null) {
                var value = parseValue(jsonValue, descriptor.getPropertyType(), descriptor.getWriteMethod().getGenericParameterTypes()[0]);

                try {
                    descriptor.getWriteMethod().invoke(instance, value);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(format(ERROR_SET_PROPERTY, descriptor.getName(), beanClass), e);
                }
            }
        }

        return instance;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object parseValue(JsonValue jsonValue, Class<?> rawType, Type genericType) {
        if (jsonValue == null || jsonValue.getValueType() == JsonValue.ValueType.NULL) {
            if (rawType == Optional.class) {
                return Optional.empty();
            }
            if (rawType.isPrimitive()) {
                return getDefaultPrimitiveValue(rawType);
            }
            return null;
        }

        if (rawType == Optional.class && genericType instanceof ParameterizedType parameterized) {
            var innerType = parameterized.getActualTypeArguments()[0];
            var innerRaw = innerType instanceof Class<?> c ? c : Object.class;
            return Optional.ofNullable(parseValue(jsonValue, innerRaw, innerType));
        }

        if (rawType == String.class) {
            return ((JsonString) jsonValue).getString();
        }

        if (rawType == char.class || rawType == Character.class) {
            var str = ((JsonString) jsonValue).getString();
            return str.isEmpty() ? '\0' : str.charAt(0);
        }

        if (rawType == boolean.class || rawType == Boolean.class) {
            return jsonValue.getValueType() == JsonValue.ValueType.TRUE;
        }

        if (rawType == int.class || rawType == Integer.class) {
            return ((JsonNumber) jsonValue).intValue();
        }

        if (rawType == long.class || rawType == Long.class) {
            return ((JsonNumber) jsonValue).longValue();
        }

        if (rawType == double.class || rawType == Double.class) {
            return ((JsonNumber) jsonValue).doubleValue();
        }

        if (rawType == float.class || rawType == Float.class) {
            return (float) ((JsonNumber) jsonValue).doubleValue();
        }

        if (rawType == short.class || rawType == Short.class) {
            return (short) ((JsonNumber) jsonValue).intValue();
        }

        if (rawType == byte.class || rawType == Byte.class) {
            return (byte) ((JsonNumber) jsonValue).intValue();
        }

        if (rawType == BigInteger.class) {
            return ((JsonNumber) jsonValue).bigIntegerValue();
        }

        if (rawType == BigDecimal.class) {
            return ((JsonNumber) jsonValue).bigDecimalValue();
        }

        if (rawType.isEnum()) {
            return Enum.valueOf((Class<Enum>) rawType, ((JsonString) jsonValue).getString());
        }

        if (rawType.isArray()) {
            return parseArray((JsonArray) jsonValue, rawType.getComponentType());
        }

        if (Collection.class.isAssignableFrom(rawType)) {
            return parseCollection((JsonArray) jsonValue, rawType, genericType);
        }

        if (Map.class.isAssignableFrom(rawType)) {
            return parseMap((JsonObject) jsonValue, rawType, genericType);
        }

        return parseObject((JsonObject) jsonValue, rawType);
    }

    private static Object parseArray(JsonArray jsonArray, Class<?> componentType) {
        var array = Array.newInstance(componentType, jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            Array.set(array, i, parseValue(jsonArray.get(i), componentType, componentType));
        }

        return array;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Collection<?> parseCollection(JsonArray jsonArray, Class<?> rawType, Type genericType) {
        Class<?> elementRawType = Object.class;
        Type elementType = Object.class;

        if (genericType instanceof ParameterizedType parameterized) {
            var typeArgs = parameterized.getActualTypeArguments();

            if (typeArgs.length > 0) {
                elementType = typeArgs[0];
                elementRawType = elementType instanceof Class<?> c ? c : Object.class;
            }
        }

        Collection collection;

        if (Set.class.isAssignableFrom(rawType)) {
            collection = rawType == TreeSet.class ? new TreeSet<>() : new HashSet<>();
        }
        else {
            collection = new ArrayList<>();
        }

        for (var item : jsonArray) {
            collection.add(parseValue(item, elementRawType, elementType));
        }

        return collection;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<?, ?> parseMap(JsonObject jsonObject, Class<?> rawType, Type genericType) {
        Class<?> valueRawType = Object.class;
        Type valueType = Object.class;

        if (genericType instanceof ParameterizedType parameterized) {
            var typeArgs = parameterized.getActualTypeArguments();

            if (typeArgs.length > 1) {
                valueType = typeArgs[1];
                valueRawType = valueType instanceof Class<?> c ? c : Object.class;
            }
        }

        Map map;

        if (rawType == TreeMap.class) {
            map = new TreeMap<>();
        }
        else if (rawType == LinkedHashMap.class) {
            map = new LinkedHashMap<>();
        }
        else {
            map = new HashMap<>();
        }

        for (var entry : jsonObject.entrySet()) {
            map.put(entry.getKey(), parseValue(entry.getValue(), valueRawType, valueType));
        }

        return map;
    }

    private static Object getDefaultPrimitiveValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0;
        }
        return null;
    }
}
