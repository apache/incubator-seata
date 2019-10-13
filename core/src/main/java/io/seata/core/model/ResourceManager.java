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
package io.seata.core.model;

import java.util.Map;

/**
 * Resource Manager: common behaviors.
 *
 * @author sharajava
 */
public interface ResourceManager extends ResourceManagerInbound, ResourceManagerOutbound {

    Object RESOURCE_LOCK = new Object();

    /**
     * Register a Resource to be managed by Resource Manager.
     *
     * @param resource The resource to be managed.
     */
    void registerResource(Resource resource);

    /**
     * Unregister a Resource from the Resource Manager.
     *
     * @param resource The resource to be removed.
     */
    void unregisterResource(Resource resource);

    /**
     * Get all resources managed by this manager.
     *
     * @return resourceId -> Resource Map
     */
    Map<String, Resource> getManagedResources();

    /**
     * Get the BranchType.
     *
     * @return The BranchType of ResourceManager.
     */
    BranchType getBranchType();
}
