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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.util.HashMap;
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

        JSONObject machineJsonObject = new JSONObject(true);

        List<Object> nodes = (List) designerJsonObject.get("nodes");
        if (nodes != null && nodes.size() > 0) {

            Map<String, JSONObject> nodeMap = new HashMap<>(nodes.size());

            for (Object node : nodes) {
                JSONObject nodeObj = (JSONObject) node;

                transformNode(machineJsonObject, nodeMap, nodeObj);
            }

            List<Object> edges = (List) designerJsonObject.get("edges");
            if (edges != null && edges.size() > 0) {
                for (Object edge : edges) {
                    JSONObject edgeObj = (JSONObject) edge;
                    transformEdge(machineJsonObject, nodes, nodeMap, edgeObj);
                }
            }
        }
        return machineJsonObject;
    }

    private static void transformNode(JSONObject machineJsonObject, Map<String, JSONObject> nodeMap, JSONObject nodeObj) {
        nodeMap.put(nodeObj.getString("id"), nodeObj);

        String type = nodeObj.getString("stateType");
        JSONObject propsObj = (JSONObject) nodeObj.get("stateProps");
        if ("Start".equals(type)) {
            if (propsObj != null && propsObj.containsKey("StateMachine")) {
                machineJsonObject.putAll(propsObj.getJSONObject("StateMachine"));
            }
        } else if (!"Catch".equals(type)) {

            JSONObject states = machineJsonObject.getJSONObject("States");
            if (states == null) {
                states = new JSONObject(true);
                machineJsonObject.put("States", states);
            }

            JSONObject stateJsonObject = new JSONObject(true);
            String stateId = nodeObj.getString("stateId");
            if (states.containsKey(stateId)) {
                throw new RuntimeException(
                        "Transform designer json to standard json failed, stateId[" + stateId + "] already exists, pls rename it.");
            }

            String comment = nodeObj.getString("label");
            if (StringUtils.hasLength(comment)) {
                stateJsonObject.put("Comment", comment);
            }
            if (propsObj != null) {
                stateJsonObject.putAll(propsObj);
            }

            states.put(stateId, stateJsonObject);

            String stateType = nodeObj.getString("stateType");
            if ("Compensation".equals(stateType)) {
                stateJsonObject.put("Type", "ServiceTask");
            } else {
                stateJsonObject.put("Type", stateType);
            }
        }
    }

    private static void transformEdge(JSONObject machineJsonObject, List<Object> nodes, Map<String, JSONObject> nodeMap,
                                      JSONObject edgeObj) {
        String sourceId = edgeObj.getString("source");
        String targetId = edgeObj.getString("target");
        if (StringUtils.hasLength(sourceId)) {
            JSONObject sourceNode = nodeMap.get(sourceId);
            JSONObject targetNode = nodeMap.get(targetId);

            if (sourceNode != null) {

                JSONObject states = machineJsonObject.getJSONObject("States");
                JSONObject sourceState = states.getJSONObject(sourceNode.getString("stateId"));
                String targetStateId = targetNode.getString("stateId");

                String sourceType = sourceNode.getString("stateType");
                if ("Start".equals(sourceType)) {
                    machineJsonObject.put("StartState", targetStateId);
                    //Make sure 'StartState' is before 'States'
                    machineJsonObject.put("States", machineJsonObject.remove("States"));
                } else if ("ServiceTask".equals(sourceType)) {
                    if (targetNode != null && "Compensation".equals(targetNode.getString("stateType"))) {
                        sourceState.put("CompensateState", targetStateId);
                    } else {
                        sourceState.put("Next", targetStateId);
                    }
                } else if ("Catch".equals(sourceType)) {
                    JSONObject catchAttachedNode = getCatchAttachedNode(sourceNode, nodes);
                    if (catchAttachedNode == null) {
                        throw new RuntimeException("'Catch' node[" + sourceNode.get("id") + "] is not attached on a 'ServiceTask'");
                    }
                    JSONObject catchAttachedState = (JSONObject) states.get(catchAttachedNode.getString("stateId"));
                    JSONArray catches = catchAttachedState.getJSONArray("Catch");
                    if (catches == null) {
                        catches = new JSONArray();
                        catchAttachedState.put("Catch", catches);
                    }

                    JSONObject edgeProps = (JSONObject) edgeObj.get("stateProps");
                    if (edgeProps != null) {
                        JSONObject catchObj = new JSONObject(true);
                        catchObj.put("Exceptions", edgeProps.get("Exceptions"));
                        catchObj.put("Next", targetStateId);
                        catches.add(catchObj);
                    }
                } else if ("Choice".equals(sourceType)) {
                    JSONArray choices = sourceState.getJSONArray("Choices");
                    if (choices == null) {
                        choices = new JSONArray();
                        sourceState.put("Choices", choices);
                    }

                    JSONObject edgeProps = (JSONObject) edgeObj.get("stateProps");
                    if (edgeProps != null) {

                        if (Boolean.TRUE.equals(edgeProps.getBoolean("Default"))) {
                            sourceState.put("Default", targetStateId);
                        } else {
                            JSONObject choiceObj = new JSONObject(true);
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

    private static JSONObject getCatchAttachedNode(JSONObject catchNode, List<Object> nodes) {
        int catchNodeX = catchNode.getInteger("x");
        int catchNodeY = catchNode.getInteger("y");
        String catchSize = catchNode.getString("size");
        String[] catchSizes = catchSize.split("\\*");
        int catchWidth = Integer.parseInt(catchSizes[0]);
        int catchHeight = Integer.parseInt(catchSizes[1]);

        for (Object node : nodes) {
            JSONObject nodeObj = (JSONObject) node;
            if (catchNode != nodeObj && "ServiceTask".equals(nodeObj.get("stateType"))) {

                int nodeX = nodeObj.getInteger("x");
                int nodeY = nodeObj.getInteger("y");

                String nodeSize = nodeObj.getString("size");
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

    private static boolean isBordersCoincided(int xyA, int xyB, int lengthA, int lengthB) {
        int centerPointLength = xyA > xyB ? xyA - xyB : xyB - xyA;
        return ((lengthA + lengthB) / 2) > centerPointLength;
    }
}