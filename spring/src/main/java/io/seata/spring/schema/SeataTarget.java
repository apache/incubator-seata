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
package io.seata.spring.schema;

import java.util.Objects;

/**
 * The type seata target
 *
 * @author xingfudeshi@gmail.com
 */
public class SeataTarget {
    private SeataTargetType seataTargetType;
    private String targetName;
    private GlobalTransactionalConfig globalTransactionalConfig;

    public SeataTargetType getSeataTargetType() {
        return seataTargetType;
    }

    public void setSeataTargetType(SeataTargetType seataTargetType) {
        this.seataTargetType = seataTargetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public GlobalTransactionalConfig getGlobalTransactionalConfig() {
        return globalTransactionalConfig;
    }

    public void setGlobalTransactionalConfig(GlobalTransactionalConfig globalTransactionalConfig) {
        this.globalTransactionalConfig = globalTransactionalConfig;
    }

    @Override
    public String toString() {
        return "SeataTarget{" +
            "seataTargetType=" + seataTargetType +
            ", targetName='" + targetName + '\'' +
            ", globalTransactionalConfig=" + globalTransactionalConfig +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SeataTarget that = (SeataTarget) o;
        return seataTargetType == that.seataTargetType && Objects.equals(targetName, that.targetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seataTargetType, targetName);
    }
}
