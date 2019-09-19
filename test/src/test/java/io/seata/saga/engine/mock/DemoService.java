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
package io.seata.saga.engine.mock;

import io.seata.saga.engine.exception.EngineExecutionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lorne.cl
 */
public class DemoService {

    public Map<String, Object> foo(Map<String, Object> input) {
        if(input == null){
            return new HashMap<>(0);
        }
        if("true".equals(input.get("throwException"))){
            throw new EngineExecutionException("foo execute failed");
        }
        if("true".equals(input.get("throwExceptionRandomly"))){
            if(Math.random() > 0.5){
                throw new EngineExecutionException("foo execute failed");
            }
        }
        return input;
    }

    public Map<String, Object> compensateFoo(Map<String, Object> input) {
        if(input == null){
            return new HashMap<>(0);
        }
        if("true".equals(input.get("throwException"))){
            throw new EngineExecutionException("compensateFoo execute failed");
        }
        if("true".equals(input.get("throwExceptionRandomly"))){
            if(Math.random() > 0.8){
                throw new EngineExecutionException("compensateFoo execute failed");
            }
        }
        return input;
    }

    public Map<String, Object> bar(Map<String, Object> input) {
        if(input == null){
            return new HashMap<>(0);
        }
        if("true".equals(input.get("throwException"))){
            throw new EngineExecutionException("bar execute failed");
        }
        if("true".equals(input.get("throwExceptionRandomly"))){
            if(Math.random() > 0.5){
                throw new EngineExecutionException("bar execute failed");
            }
        }
        return input;
    }

    public Map<String, Object> compensateBar(Map<String, Object> input) {
        if(input == null){
            return new HashMap<>(0);
        }
        if("true".equals(input.get("throwException"))){
            throw new EngineExecutionException("compensateBar execute failed");
        }
        if("true".equals(input.get("throwExceptionRandomly"))){
            if(Math.random() > 0.8){
                throw new EngineExecutionException("compensateBar execute failed");
            }
        }
        return input;
    }

    public People complexParameterMethod(String name, int age, People people, People[] peopleArrya, List<People> peopleList, Map<String, People> peopleMap){
        return people;
    }

    public static class People {

        private String name;
        private int    age;

        private People[] childrenArray;
        private List<People> childrenList;
        private Map<String, People> childrenMap;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public People[] getChildrenArray() {
            return childrenArray;
        }

        public void setChildrenArray(People[] childrenArray) {
            this.childrenArray = childrenArray;
        }

        public List<People> getChildrenList() {
            return childrenList;
        }

        public void setChildrenList(List<People> childrenList) {
            this.childrenList = childrenList;
        }

        public Map<String, People> getChildrenMap() {
            return childrenMap;
        }

        public void setChildrenMap(Map<String, People> childrenMap) {
            this.childrenMap = childrenMap;
        }
    }
}
