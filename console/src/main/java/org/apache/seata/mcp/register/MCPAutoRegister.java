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
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.annotation.Prompt;
import org.apache.seata.mcp.annotation.PromptParam;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.manager.MCPServerManager;
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
import java.util.*;

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

    // Type tracking to prevent circular references
    private final Set<Class<?>> processingTypes = new HashSet<>();

    public MCPAutoRegister(MCPServerManager aysncManager) {
        this.aysncManager = aysncManager;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        for (Method m : bean.getClass().getMethods()) {
            Tool toolAnn = m.getAnnotation(Tool.class);
            Prompt promptAnn = m.getAnnotation(Prompt.class);
            if(toolAnn!=null){
                autoRegisterTool(bean,m,toolAnn);
            }

            if(promptAnn!=null){
                autoRegisterPrompt(bean,m,promptAnn);
            }
        }

        return bean;
    }

    public void autoRegisterPrompt(Object bean, Method m, Prompt ann){
        Parameter[] methodParams = m.getParameters();
        List<McpSchema.PromptArgument> arguments = new ArrayList<>();
        for (Parameter p : methodParams) {
            PromptParam paramAnn = p.getAnnotation(PromptParam.class);
            McpSchema.PromptArgument promptArgument = new McpSchema.PromptArgument();
            promptArgument.setName(p.getName());
            if(paramAnn!=null){
                if(StringUtils.isNotBlank(paramAnn.description())){
                    promptArgument.setDescription(paramAnn.description());
                }
                promptArgument.setRequired(paramAnn.required());
            }
            arguments.add(promptArgument);
        }

        // —— 2. build ToolSpecification and register as a tool ——
        McpSchema.Prompt promptMeta = new McpSchema.Prompt(m.getName(), ann.description(), arguments);

        McpServerFeatures.AsyncPromptSpecification spec = new McpServerFeatures.AsyncPromptSpecification(
                promptMeta, (exchange, request) -> Mono.fromCallable(() -> {
                    try {
                        Object[] args = Arrays.stream(methodParams)
                                .map(p -> convertArgument(request.getArguments().get(p.getName()), p.getType()))
                                .toArray();

                        Object ret = m.invoke(bean, args);
                        List<McpSchema.PromptMessage> messages = new ArrayList<>();
                        if (ret instanceof McpSchema.GetPromptResult) {
                            return (McpSchema.GetPromptResult) ret;
                        } else if (ret instanceof String) {
                            messages.add(new McpSchema.PromptMessage(McpSchema.Role.USER,new McpSchema.TextContent((String) ret)));
                        }

                        // `false` This call will no longer trigger the LLM to continue calling the tool
                        return new McpSchema.GetPromptResult("",messages);

                    } catch (InvocationTargetException ite) {
                        String err = ite.getTargetException().getMessage();
                        return new McpSchema.GetPromptResult("error",Collections.singletonList(new McpSchema.PromptMessage(McpSchema.Role.USER,new McpSchema.TextContent(err))));
                    } catch (Exception e) {
                        logger.error("Prompt transform failed:{}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic()));

        // Add a prompt and process the returned Mono
        aysncManager
                .getServerInstance()
                .addPrompt(spec)
                .doOnError(error -> {
                    logger.error("Prompt registration failed:{}", error.getMessage());
                    throw new RuntimeException("Failed to register the prompt: " + m.getName(), error);
                })
                .subscribe();
    }

    public void autoRegisterTool(Object bean, Method m, Tool ann){
        // —— 1. Dynamically generate JSON Schema -
        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("type", "object");

        ObjectNode props = parameters.putObject("properties");
        ArrayNode required = parameters.putArray("required");

        Parameter[] methodParams = m.getParameters();
        for (Parameter p : methodParams) {
            String pName = p.getName();
            Class<?> pt = p.getType();

            // Cleanup the collection of processing type tracks
            processingTypes.clear();

            // Generate a schema for the parameters
            ObjectNode prop = generatePropertySchema(pt, p);
            props.set(pName, prop);

            // Check whether this parameter is mandatory
            ToolParam paramAnn = p.getAnnotation(ToolParam.class);
            if (paramAnn == null || paramAnn.required()) {
                required.add(pName);
            }
        }

        String schemaStr = parameters.toString();

        // —— 2. build ToolSpecification and register as a tool ——
        McpSchema.Tool toolMeta = new McpSchema.Tool(m.getName(), ann.description(), schemaStr);

        McpServerFeatures.AsyncToolSpecification spec = McpServerFeatures.AsyncToolSpecification.builder()
                        .tool(toolMeta)
                        .callHandler((exchange,request) -> Mono.fromCallable(() -> {
                                    try {
                                        Object[] args = Arrays.stream(methodParams)
                                                .map(p -> convertArgument(request.getArguments().get(p.getName()), p.getType()))
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

                                        // `false` This call will no longer trigger the LLM to continue calling the tool
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
                                .subscribeOn(Schedulers.boundedElastic())
                        )
                .build();

        // Add a tool and process the returned Mono
        aysncManager
                .getServerInstance()
                .addTool(spec)
                .doOnError(error -> {
                    logger.error("Tool registration failed:{}", error.getMessage());
                    throw new RuntimeException("Failed to register the tool: " + m.getName(), error);
                })
                .subscribe();
    }

    /**
     * Generate JSON Schema attributes for the parameters
     */
    private ObjectNode generatePropertySchema(Class<?> type, Parameter parameter) {
        ObjectNode prop = mapper.createObjectNode();

        // Get @ToolParam annotations
        ToolParam paramAnn = parameter.getAnnotation(ToolParam.class);
        if (paramAnn != null && !paramAnn.description().isEmpty()) {
            prop.put("description", paramAnn.description());
        }

        // Recursively generate schemas
        generateTypeSchema(prop, type);

        return prop;
    }

    /**
     * A JSON schema of the recursive generation type
     */
    private void generateTypeSchema(ObjectNode prop, Class<?> type) {
        // Prevent circular references
        if (processingTypes.contains(type)) {
            prop.put("type", "object");
            prop.put("description", "Circular references: " + type.getSimpleName());
            return;
        }

        // Basic type mapping
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
            // Custom object types
            prop.put("type", "object");

            // Prevent circular references
            processingTypes.add(type);

            try {
                ObjectNode properties = prop.putObject("properties");
                ArrayNode required = prop.putArray("required");

                // GET ALL THE FIELDS
                Field[] fields = getAllFields(type);

                for (Field field : fields) {
                    // Skip static fields and final fields
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        continue;
                    }

                    String fieldName = field.getName();
                    ObjectNode fieldProp = properties.putObject(fieldName);

                    // Check the @ToolParam annotation of the field
                    ToolParam fieldAnn = field.getAnnotation(ToolParam.class);
                    if (fieldAnn != null) {
                        if (!fieldAnn.description().isEmpty()) {
                            fieldProp.put("description", fieldAnn.description());
                        }
                        if (!fieldAnn.example().isEmpty()) {
                            fieldProp.put("example", fieldAnn.example());
                        }
                        if (fieldAnn.exampleValueClassName() != null && fieldAnn.exampleValueClassName().length != 0) {
                            // The conversion type is JSON format
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

                    // Recursively generate a schema of field types
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

    /**
     * Converts all field information of a class to JSON strings(Include Enum)
     */
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

                    // Trying to get code value (for GlobalStatus, etc.)
                    try {
                        Method getCodeMethod = clazz.getMethod("getCode");
                        Object code = getCodeMethod.invoke(enumConstant);
                        enumInfo.put("code", code.toString());
                    } catch (Exception ignored) {
                        // Ignore enumerations that don't have a getCode method
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
                    if (!annotation.description().isEmpty()) {
                        fieldInfo.put("description", annotation.description());
                    }
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

    /**
     * Get all fields of a class (including parent class fields)
     */
    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields.toArray(new Field[0]);
    }

    /**
     * Determine whether the object type is a custom object
     */
    private boolean isCustomObject(Class<?> type) {
        // Exclude Java built-in types
        return !type.isPrimitive()
                && !type.getName().startsWith("java.")
                && !type.getName().startsWith("javax.")
                && !type.isEnum()
                && !type.isInterface()
                && !type.isArray();
    }

    /**
     * Conversion parameter type (used for parameter conversion on method call)
     */
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
                    "Parameter conversion failed: value=" + arg + " (" + arg.getClass().getSimpleName() + ") -> " + targetType,
                    e
            );
        }
    }
}
