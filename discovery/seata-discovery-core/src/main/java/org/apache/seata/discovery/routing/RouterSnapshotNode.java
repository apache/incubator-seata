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
package org.apache.seata.discovery.routing;

import java.util.List;

/**
 * Used to record snapshot information during router execution
 *
 * @param <T> service instance type
 */
public class RouterSnapshotNode<T> {

    private final String routerName;
    private final int inputSize;
    private final int outputSize;
    private final List<T> selectedServers;
    private final long executionTimeMs;

    /**
     * Constructor
     *
     * @param routerName      router name
     * @param inputSize       input size
     * @param outputSize      output size
     * @param selectedServers selected server list
     * @param executionTimeMs execution time in milliseconds
     */
    public RouterSnapshotNode(
            String routerName, int inputSize, int outputSize, List<T> selectedServers, long executionTimeMs) {
        this.routerName = routerName;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.selectedServers = selectedServers;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Get router name
     *
     * @return router name
     */
    public String getRouterName() {
        return routerName;
    }

    /**
     * Get input size
     *
     * @return input size
     */
    public int getInputSize() {
        return inputSize;
    }

    /**
     * Get output size
     *
     * @return output size
     */
    public int getOutputSize() {
        return outputSize;
    }

    /**
     * Get selected server list
     *
     * @return selected server list
     */
    public List<T> getSelectedServers() {
        return selectedServers;
    }

    /**
     * Get execution time in milliseconds
     *
     * @return execution time
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "%s: input=%d, output=%d, time=%dms", routerName, inputSize, outputSize, executionTimeMs));

        if (selectedServers != null && !selectedServers.isEmpty() && selectedServers.size() <= 3) {
            sb.append(", selected=").append(selectedServers);
        }

        return sb.toString();
    }
}
