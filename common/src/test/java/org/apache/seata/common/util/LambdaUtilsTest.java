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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LambdaUtilsTest {

    @Test
    void shouldReturnTrueForDistinctKeys() {
        Function<Object, Object> keyExtractor = Object::getClass;
        Predicate<Object> distinctByKey = LambdaUtils.distinctByKey(keyExtractor);

        boolean result = distinctByKey.test(new Object());
        assertTrue(result);
    }

    @Test
    void testDistinctByKeyWithDuplicateValues() {
        // Create a predicate to check distinct names
        Predicate<Person> distinctByName = LambdaUtils.distinctByKey(Person::getName);

        // Test with different person objects with same name
        Person person1 = new Person("Alice", 25);
        Person person2 = new Person("Alice", 30); // Same name, different age
        Person person3 = new Person("Bob", 35); // Different name

        // First time should return true
        assertTrue(distinctByName.test(person1));

        // Second time with same name should return false
        assertFalse(distinctByName.test(person2));

        // Different name should return true
        assertTrue(distinctByName.test(person3));
    }

    @Test
    void testDistinctByKeyWithStream() {
        // Create a list of persons with some duplicate names
        Person[] persons = {
            new Person("Alice", 25),
            new Person("Bob", 30),
            new Person("Alice", 35), // Duplicate name
            new Person("Charlie", 40),
            new Person("Bob", 45) // Duplicate name
        };

        // Filter distinct persons by name
        Predicate<Person> distinctByName = LambdaUtils.distinctByKey(Person::getName);
        long distinctCount = Stream.of(persons).filter(distinctByName).count();

        // Should have 3 distinct names: Alice, Bob, Charlie
        assertTrue(distinctCount == 3);
    }

    // Helper class for testing
    static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
