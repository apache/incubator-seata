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
package io.seata.server.console.vo;

import java.util.Map;

/**
 * GlobalLockVO
 * @author Yuzhiqiang
 */
public class GlobalConfigVO {

    private Integer id;

    private String name;

    private String value;

    private Map<String, String> descrMap;

    public GlobalConfigVO() {

    }

    public GlobalConfigVO(Integer id, String name, String value, Map<String, String> descrMap) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.descrMap = descrMap;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, String> getDescrMap() {
        return descrMap;
    }

    public void setDescrMap(Map<String, String> descrMap) {
        this.descrMap = descrMap;
    }
}
