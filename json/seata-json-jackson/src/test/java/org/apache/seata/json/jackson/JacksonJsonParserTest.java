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
package org.apache.seata.json.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonJsonParserTest {

    private JacksonJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new JacksonJsonParser();
    }

    static class Person {
        private String name;
        private int age;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    interface Animal {
        String makeSound();
    }

    static class Cat implements Animal {
        public int lives = 9;

        @Override
        public String makeSound() {
            return "Meow";
        }
    }

    static class Zoo {
        private Animal animal;
        public String name;

        public Animal getAnimal() {
            return animal;
        }

        public void setAnimal(Animal animal) {
            this.animal = animal;
        }
    }

    @Test
    void toJSONStringShouldContainAutoTypeByDefault() {
        // Arrange
        Person person = new Person();
        person.setName("Jack");

        // Act
        String json = parser.toJSONString(person);

        // Assert
        assertThat(json).contains("\"@type\"");
    }

    @Test
    void toJsonStringShouldNotContainAutoTypeWhenIgnored() {
        // Arrange
        Person person = new Person();
        person.setName("Jack");

        // Act
        String json = parser.toJsonString(person, true, false);

        // Assert
        assertThat(json).doesNotContain("\"@type\"");
    }

    @Test
    void parseShouldIgnoreAutoTypeByDefault() {
        // Arrange
        String jsonWithAutoType = "{\"@type\":\"" + Person.class.getName() + "\",\"name\":\"Jill\",\"age\":30}";
        Person expected = new Person();
        expected.setName("Jill");
        expected.setAge(30);

        // Act
        Person actual = parser.parse(jsonWithAutoType, Person.class);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parseShouldHandlePolymorphicObjectWhenAutoTypeIsEnabled() {
        // Arrange
        Zoo zoo = new Zoo();
        zoo.name = "City Zoo";
        zoo.setAnimal(new Cat());
        String json = parser.toJsonString(zoo, false, false);

        // Act
        Zoo deserializedZoo = parser.parse(json, Zoo.class, false);

        // Assert
        assertThat(deserializedZoo).isNotNull();
        assertThat(deserializedZoo.getAnimal()).isNotNull().isInstanceOf(Cat.class);
        assertThat(((Cat) deserializedZoo.getAnimal()).lives).isEqualTo(9);
    }

    @Test
    void shouldSerializeEmptyListToString() {
        // Arrange
        List<String> emptyList = Collections.emptyList();

        // Act
        String json = parser.toJsonString(emptyList, false, false);

        // Assert
        assertThat(json).isEqualTo("[]");
    }

    @Test
    void shouldDeserializeEmptyListString() {
        // Arrange
        String json = "[]";

        // Act
        List<?> result = parser.parse(json, List.class, true);

        // Assert
        assertThat(result).isInstanceOf(ArrayList.class).isEmpty();
    }

    @Test
    void parseShouldThrowRuntimeExceptionOnMalformedJson() {
        // Arrange
        String malformedJson = "{\"name\":\"John\", \"age\":30"; // Missing closing brace

        // Act & Assert - Using AssertJ's fluent exception testing
        assertThatThrownBy(() -> parser.parse(malformedJson, Person.class, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Parse json to object error")
                .hasCauseInstanceOf(IOException.class);
    }
}
