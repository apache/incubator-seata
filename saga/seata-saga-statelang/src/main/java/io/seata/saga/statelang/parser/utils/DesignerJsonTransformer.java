/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.saga.statelang.parser.utils;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.parser.JsonParser;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform designer json to standard Saga State language json
 *
 * @author lorne.cl
 */
public class DesignerJsonTransformer {

    public static Map<String, Object> toStandardJson(Map<String, Object> designerJsonObject) {

        if (!isDesignerJson(designerJsonObject)) {
            return designerJsonObject;
        }
        Map<String, Object> machineJsonObject = new LinkedHashMap<>();

        List<Object> nodes = (List) designerJsonObject.get("nodes");
        if (CollectionUtils.isNotEmpty(nodes)) {
            Map<String, Object> nodeMap = new LinkedHashMap<>(nodes.size());

            for (Object node : nodes) {
                Map<String, Object> nodeObj = (Map<String, Object>) node;

                transformNode(machineJsonObject, nodeMap, nodeObj);
            }

            List<Object> edges = (List) designerJsonObject.get("edges");
            if (CollectionUtils.isNotEmpty(edges)) {
                for (Object edge : edges) {
                    Map<String, Object> edgeObj = (Map<String, Object>) edge;
                    transformEdge(machineJsonObject, nodes, nodeMap, edgeObj);
                }
            }
        }
        return machineJsonObject;
    }

    private static void transformNode(Map<String, Object> machineJsonObject, Map<String, Object> nodeMap, Map<String, Object> nodeObj) {
        nodeMap.put((String) nodeObj.get("id"), nodeObj);

        String type = (String) nodeObj.get("stateType");
        Map<String, Object> propsObj = (Map<String, Object>) nodeObj.get("stateProps");
        if ("Start".equals(type)) {
            if (propsObj != null && propsObj.containsKey("StateMachine")) {
                machineJsonObject.putAll((Map<String, Object>) propsObj.get("StateMachine"));
            }
        } else if (!"Catch".equals(type)) {
            Map<String, Object> states = (Map<String, Object>) CollectionUtils.computeIfAbsent(machineJsonObject, "States",
                key -> new LinkedHashMap<>());

            Map<String, Object> stateJsonObject = new LinkedHashMap<>();
            String stateId = (String) nodeObj.get("stateId");
            if (states.containsKey(stateId)) {
                throw new RuntimeException(
                        "Transform designer json to standard json failed, stateId[" + stateId + "] already exists, pls rename it.");
            }

            String comment = (String) nodeObj.get("label");
            if (StringUtils.hasLength(comment)) {
                stateJsonObject.put("Comment", comment);
            }
            if (propsObj != null) {
                stateJsonObject.putAll(propsObj);
            }

            states.put(stateId, stateJsonObject);

            String stateType = (String) nodeObj.get("stateType");
            if ("Compensation".equals(stateType)) {
                stateJsonObject.put("Type", "ServiceTask");
            } else {
                stateJsonObject.put("Type", stateType);
            }
        }
    }

    @SuppressWarnings("lgtm[java/dereferenced-value-may-be-null]")
    private static void transformEdge(Map<String, Object> machineJsonObject, List<Object> nodes, Map<String, Object> nodeMap, Map<String, Object> edgeObj) {
        String sourceId = (String) edgeObj.get("source");
        String targetId = (String) edgeObj.get("target");
        if (StringUtils.hasLength(sourceId)) {
            Map<String, Object> sourceNode = (Map<String, Object>) nodeMap.get(sourceId);
            Map<String, Object> targetNode = (Map<String, Object>) nodeMap.get(targetId);

            if (sourceNode != null) {
                Map<String, Object> states = (Map<String, Object>) machineJsonObject.get("States");
                Map<String, Object> sourceState = (Map<String, Object>) states.get((String) sourceNode.get("stateId"));
                String targetStateId = (String) targetNode.get("stateId");

                String sourceType = (String) sourceNode.get("stateType");
                if ("Start".equals(sourceType)) {
                    machineJsonObject.put("StartState", targetStateId);
                    //Make sure 'StartState' is before 'States'
                    machineJsonObject.put("States", machineJsonObject.remove("States"));
                } else if ("ServiceTask".equals(sourceType)) {
                    if (targetNode != null && "Compensation".equals(targetNode.get("stateType"))) {
                        sourceState.put("CompensateState", targetStateId);
                    } else {
                        sourceState.put("Next", targetStateId);
                    }
                } else if ("Catch".equals(sourceType)) {
                    Map<String, Object> catchAttachedNode = getCatchAttachedNode(sourceNode, nodes);
                    if (catchAttachedNode == null) {
                        throw new RuntimeException("'Catch' node[" + sourceNode.get("id") + "] is not attached on a 'ServiceTask' or 'ScriptTask'");
                    }
                    Map<String, Object> catchAttachedState = (Map<String, Object>) states.get(catchAttachedNode.get("stateId"));
                    List<Object> catches = (List<Object>) CollectionUtils.computeIfAbsent(catchAttachedState, "Catch",
                        key -> new ArrayList<>());

                    Map<String, Object> edgeProps = (Map<String, Object>) edgeObj.get("stateProps");
                    if (edgeProps != null) {
                        Map<String, Object> catchObj = new LinkedHashMap<>();
                        catchObj.put("Exceptions", edgeProps.get("Exceptions"));
                        catchObj.put("Next", targetStateId);
                        catches.add(catchObj);
                    }
                } else if ("Choice".equals(sourceType)) {
                    List<Object> choices = (List<Object>) CollectionUtils.computeIfAbsent(sourceState, "Choices",
                        key -> new ArrayList<>());

                    Map<String, Object> edgeProps = (Map<String, Object>) edgeObj.get("stateProps");
                    if (edgeProps != null) {
                        if (Boolean.TRUE.equals(edgeProps.get("Default"))) {
                            sourceState.put("Default", targetStateId);
                        } else {
                            Map<String, Object> choiceObj = new LinkedHashMap<>();
                            choiceObj.put("Expression", edgeProps.get("Expression"));
                            choiceObj.put("Next", targetStateId);
                            choices.add(choiceObj);
                        }
                    }
                } else {
                    sourceState.put("Next", targetStateId);
                }
            }
        }
    }

    public static boolean isDesignerJson(Map<String, Object> jsonObject) {
        return jsonObject != null && jsonObject.containsKey("nodes") && jsonObject.containsKey("edges");
    }

    private static Map<String, Object> getCatchAttachedNode(Map<String, Object> catchNode, List<Object> nodes) {
        Number catchNodeX = (Number) catchNode.get("x");
        Number catchNodeY = (Number) catchNode.get("y");
        String catchSize = (String) catchNode.get("size");
        String[] catchSizes = catchSize.split("\\*");
        int catchWidth = Integer.parseInt(catchSizes[0]);
        int catchHeight = Integer.parseInt(catchSizes[1]);

        for (Object node : nodes) {
            Map<String, Object> nodeObj = (Map<String, Object>) node;
            if (catchNode != nodeObj &&
                    ("ServiceTask".equals(nodeObj.get("stateType"))
                            || "ScriptTask".equals(nodeObj.get("stateType")))) {

                Number nodeX = (Number) nodeObj.get("x");
                Number nodeY = (Number) nodeObj.get("y");

                String nodeSize = (String) nodeObj.get("size");
                String[] nodeSizes = nodeSize.split("\\*");
                int nodeWidth = Integer.parseInt(nodeSizes[0]);
                int nodeHeight = Integer.parseInt(nodeSizes[1]);

                if (isBordersCoincided(catchNodeX, nodeX, catchWidth, nodeWidth)
                        && isBordersCoincided(catchNodeY, nodeY, catchHeight, nodeHeight)) {

                    return nodeObj;
                }
            }
        }
        return null;
    }

    private static boolean isBordersCoincided(Number xyA, Number xyB, Number lengthA, Number lengthB) {
        double centerPointLength = xyA.doubleValue() > xyB.doubleValue() ? xyA.doubleValue() - xyB.doubleValue() : xyB.doubleValue() - xyA.doubleValue();
        return ((lengthA.doubleValue() + lengthB.doubleValue()) / 2) > centerPointLength;
    }

    /**
     * Generate tracing graph json
     * @param stateMachineInstance the state machine instance
     * @param jsonParser the json parser
     * @return the tracing graph json
     */
    @SuppressWarnings("lgtm[java/dereferenced-value-may-be-null]")
    public static String generateTracingGraphJson(StateMachineInstance stateMachineInstance, JsonParser jsonParser) {

        if (stateMachineInstance == null) {
            throw new FrameworkException("StateMachineInstance is not exits",
                    FrameworkErrorCode.StateMachineInstanceNotExists);
        }
        String stateMachineJson = stateMachineInstance.getStateMachine().getContent();
        if (StringUtils.isEmpty(stateMachineJson)) {
            throw new FrameworkException("Cannot get StateMachine Json",
                    FrameworkErrorCode.ObjectNotExists);
        }

        Map<String, Object> stateMachineJsonObj = jsonParser.parse(stateMachineJson, Map.class, true);
        if (!DesignerJsonTransformer.isDesignerJson(stateMachineJsonObj)) {
            throw new FrameworkException("StateMachine Json is not generated by Designer",
                    FrameworkErrorCode.InvalidConfiguration);
        }
        Map<String, List<StateInstance>> stateInstanceMapGroupByName = new HashMap<>(stateMachineInstance.getStateMap().size());
        for (StateInstance stateInstance : stateMachineInstance.getStateMap().values()) {
            CollectionUtils.computeIfAbsent(stateInstanceMapGroupByName, stateInstance.getName(), key -> new ArrayList<>())
                    .add(stateInstance);
        }
        List<Object> nodesArray = (List<Object>) stateMachineJsonObj.get("nodes");
        for (Object nodeObj : nodesArray) {
            Map<String, Object> node = (Map<String, Object>) nodeObj;
            String stateId = (String) node.get("stateId");
            String stateType = (String) node.get("stateType");
            if ("ServiceTask".equals(stateType)
                    || "SubStateMachine".equals(stateType)
                    || "Compensation".equals(stateType)) {
                node.remove("color");
            }
            List<StateInstance> stateInstanceList = stateInstanceMapGroupByName.get(stateId);
            if (CollectionUtils.isNotEmpty(stateInstanceList)) {
                StateInstance stateInstance = null;
                if (stateInstanceList.size() == 1) {
                    stateInstance = stateInstanceList.get(0);
                } else {
                    //find out latest stateInstance
                    for (StateInstance stateInst : stateInstanceList) {

                        if (stateInstance == null
                                || stateInst.getGmtStarted().after(stateInstance.getGmtStarted())) {
                            stateInstance = stateInst;
                        }
                    }
                }
                node.put("stateInstanceId", stateInstance.getId());
                node.put("stateInstanceStatus", stateInstance.getStatus());
                if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    node.put("color", "green");
                    Map<String, Object> style = new LinkedHashMap<>();
                    style.put("fill", "#00D73E");
                    style.put("lineWidth", 2);
                    node.put("style", style);
                } else {
                    node.put("color", "red");
                    Map<String, Object> style = new LinkedHashMap<>();
                    style.put("fill", "#FF7777");
                    style.put("lineWidth", 2);
                    node.put("style", style);
                }
            }
        }

        if (stateMachineJsonObj != null) { /*lgtm[java/useless-null-check]*/
            return jsonParser.toJsonString(stateMachineJsonObj, true);
        }
        return "";
    }
}