/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.manager.MCPServerManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Automatic tool registration class, able to scan @McpTool and @McpParam annotations, encapsulating them as mcp specified Json schema
 * Attribute mapping of object type parameters is supported
 * @author xb2555
 */
@Component
public class MCPAutoRegister implements BeanPostProcessor {

    private final MCPServerManager aysncManager;

    @Autowired
    private ObjectMapper mapper;

    private final Logger logger = LoggerFactory.getLogger(MCPAutoRegister.class);

    private final Set<Class<?>> processingTypes = new HashSet<>();

    public MCPAutoRegister(MCPServerManager aysncManager) {
        this.aysncManager = aysncManager;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, @NotNull String name) {
        for (Method m : bean.getClass().getMethods()) {
            Tool toolAnn = m.getAnnotation(Tool.class);
            if (toolAnn != null) {
                autoRegisterTool(bean, m, toolAnn);
            }
        }
        return bean;
    }

    public void autoRegisterTool(Object bean, Method m, Tool ann) {
        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("type", "object");

        ObjectNode props = parameters.putObject("properties");
        ArrayNode required = parameters.putArray("required");

        Parameter[] methodParams = m.getParameters();
        for (Parameter p : methodParams) {
            String pName = p.getName();
            Class<?> pt = p.getType();
            processingTypes.clear();
            ObjectNode prop = generatePropertySchema(pt, p);
            props.set(pName, prop);
            ToolParam paramAnn = p.getAnnotation(ToolParam.class);
            if (paramAnn != null) {
                if (paramAnn.required()) {
                    required.add(pName);
                }
            }
        }

        String schemaStr = parameters.toString();
        McpSchema.Tool toolMeta = new McpSchema.Tool(m.getName(), ann.description(), schemaStr);
        McpServerFeatures.AsyncToolSpecification spec = McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolMeta)
                .callHandler((exchange, request) -> Mono.fromCallable(() -> {
                            try {
                                Object[] args = Arrays.stream(methodParams)
                                        .map(p -> convertArgument(
                                                request.getArguments().get(p.getName()), p.getType()))
                                        .toArray();

                                Object ret = m.invoke(bean, args);

                                List<McpSchema.Content> contents = new ArrayList<>();
                                if (ret instanceof McpSchema.CallToolResult) {
                                    return (McpSchema.CallToolResult) ret;
                                } else if (ret instanceof String) {
                                    contents.add(new McpSchema.TextContent((String) ret));
                                } else {
                                    contents.add(new McpSchema.TextContent(mapper.writeValueAsString(ret)));
                                }
                                return new McpSchema.CallToolResult(contents, false);

                            } catch (InvocationTargetException ite) {
                                String err = ite.getTargetException().getMessage();
                                return new McpSchema.CallToolResult(
                                        Collections.singletonList(
                                                new McpSchema.TextContent("The tool execution error: " + err)),
                                        true);
                            } catch (Exception e) {
                                logger.error("Tool transform failed:{}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .build();
        aysncManager
                .getServerInstance()
                .addTool(spec)
                .doOnError(error -> {
                    logger.error("Tool registration failed:{}", error.getMessage());
                    throw new RuntimeException("Failed to register the tool: " + m.getName(), error);
                })
                .subscribe();
    }

    private ObjectNode generatePropertySchema(Class<?> type, Parameter parameter) {
        ObjectNode prop = mapper.createObjectNode();

        ToolParam paramAnn = parameter.getAnnotation(ToolParam.class);
        if (paramAnn != null) {
            prop.put("description", paramAnn.description());
        }

        generateTypeSchema(prop, type);

        return prop;
    }

    private void generateTypeSchema(ObjectNode prop, Class<?> type) {
        if (processingTypes.contains(type)) {
            prop.put("type", "object");
            prop.put("description", "Circular references: " + type.getSimpleName());
            return;
        }

        if (type == String.class) {
            prop.put("type", "string");
        } else if (type == Integer.class || type == int.class) {
            prop.put("type", "integer");
        } else if (type == Long.class || type == long.class) {
            prop.put("type", "integer");
        } else if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            prop.put("type", "number");
        } else if (type == Boolean.class || type == boolean.class) {
            prop.put("type", "boolean");
        } else if (type.isArray()) {
            prop.put("type", "array");
            ObjectNode items = prop.putObject("items");
            generateTypeSchema(items, type.getComponentType());
        } else if (Collection.class.isAssignableFrom(type)) {
            prop.put("type", "array");
            ObjectNode items = prop.putObject("items");
            items.put("type", "object");
        } else if (Map.class.isAssignableFrom(type)) {
            prop.put("type", "object");
            prop.put("description", "Key-value pair mappings");
        } else if (isCustomObject(type)) {
            prop.put("type", "object");
            processingTypes.add(type);

            try {
                ObjectNode properties = prop.putObject("properties");
                ArrayNode required = prop.putArray("required");

                Field[] fields = getAllFields(type);

                for (Field field : fields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        continue;
                    }

                    String fieldName = field.getName();
                    ObjectNode fieldProp = properties.putObject(fieldName);
                    ToolParam fieldAnn = field.getAnnotation(ToolParam.class);
                    if (fieldAnn != null) {
                        if (!fieldAnn.description().isEmpty()) {
                            fieldProp.put("description", fieldAnn.description());
                        }
                        if (!fieldAnn.example().isEmpty()) {
                            fieldProp.put("example", fieldAnn.example());
                        }
                        if (fieldAnn.exampleValueClassName().length != 0) {
                            StringBuilder example = new StringBuilder();
                            for (Class<?> clazz : fieldAnn.exampleValueClassName()) {
                                example.append(",").append(getClassInfoAsJson(clazz));
                            }
                            fieldProp.put("example", example.toString());
                        }
                        if (fieldAnn.required()) {
                            required.add(fieldName);
                        }
                    }
                    generateTypeSchema(fieldProp, field.getType());
                }

            } finally {
                processingTypes.remove(type);
            }
        } else {
            prop.put("type", "object");
            prop.put("description", "COMPLEX OBJECT TYPES: " + type.getSimpleName());
        }
    }

    private String getClassInfoAsJson(Class<?> clazz) {
        try {
            ObjectNode result = mapper.createObjectNode();

            result.put("className", clazz.getSimpleName());
            result.put("fullName", clazz.getName());

            if (clazz.isEnum()) {
                ArrayNode enumValues = result.putArray("enumValues");
                for (Object enumConstant : clazz.getEnumConstants()) {
                    ObjectNode enumInfo = enumValues.addObject();
                    enumInfo.put("name", enumConstant.toString());
                    enumInfo.put("ordinal", ((Enum<?>) enumConstant).ordinal());
                    try {
                        Method getCodeMethod = clazz.getMethod("getCode");
                        Object code = getCodeMethod.invoke(enumConstant);
                        enumInfo.put("code", code.toString());
                    } catch (Exception ignored) {
                    }
                }
            }

            ArrayNode fieldsArray = result.putArray("fields");
            for (Field field : getAllFields(clazz)) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                ObjectNode fieldInfo = fieldsArray.addObject();
                fieldInfo.put("name", field.getName());
                fieldInfo.put("type", field.getType().getName());
                ToolParam annotation = field.getAnnotation(ToolParam.class);
                if (annotation != null) {
                    fieldInfo.put("description", annotation.description());
                    fieldInfo.put("required", annotation.required());
                    if (!annotation.example().isEmpty()) {
                        fieldInfo.put("example", annotation.example());
                    }
                }
            }

            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Converts all field information of a class to JSON strings Failed:{}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields.toArray(new Field[0]);
    }

    private boolean isCustomObject(Class<?> type) {
        return !type.isPrimitive()
                && !type.getName().startsWith("java.")
                && !type.getName().startsWith("javax.")
                && !type.isEnum()
                && !type.isArray();
    }

    private Object convertArgument(Object arg, Class<?> targetType) {
        if (arg == null) {
            return null;
        }

        if (targetType.isAssignableFrom(arg.getClass())) {
            return arg;
        }

        try {
            return mapper.convertValue(arg, targetType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Parameter conversion failed: value=" + arg + " ("
                            + arg.getClass().getSimpleName() + ") -> " + targetType,
                    e);
        }
    }
}
