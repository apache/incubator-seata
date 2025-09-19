package org.apache.seata.json.fastjson;

import org.apache.seata.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class FastjsonParserTest {
    private FastjsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new FastjsonParser();
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

    interface Animal {
        String makeSound();
    }

    static class Dog implements Animal {
        public String breed = "Golden Retriever";

        @Override
        public String makeSound() {
            return "Woof";
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
    void shouldReturnCorrectParserName() {
        assertThat(parser.getName()).isEqualTo(Constants.FASTJSON_JSON_PARSER_NAME);
    }

    @Test
    void useAutoTypeShouldDetectCorrectly() {
        // Arrange
        String jsonWithAutoType = "{\"@type\":\"com.example.MyClass\",\"key\":\"value\"}";
        String jsonWithoutAutoType = "{\"key\":\"value\"}";

        // Act & Assert
        assertThat(parser.useAutoType(jsonWithAutoType)).isTrue();
        assertThat(parser.useAutoType(jsonWithoutAutoType)).isFalse();
        assertThat(parser.useAutoType(null)).isFalse();
    }

    @Test
    void toJSONStringShouldNotContainAutoTypeByDefault() {
        // Arrange
        Person person = new Person();
        person.setName("Jack");

        // Act
        String json = parser.toJSONString(person);

        // Assert - Using AssertJ for more fluent assertions
        assertThat(json).isNotNull().doesNotContain("@type").contains("\"name\":\"Jack\"");
    }

    @Test
    void toJsonStringShouldContainAutoTypeWhenSpecified() {
        // Arrange
        Person person = new Person();
        person.setName("Rei");

        // Act
        String json = parser.toJsonString(person, false, false);

        // Assert
        assertThat(json).contains("\"@type\":\"" + Person.class.getName() + "\"");
    }

    @Test
    void toJsonStringShouldBePrettyFormattedWhenSpecified() {
        // Arrange
        Person person = new Person();
        person.setName("Shinji");

        // Act
        String json = parser.toJsonString(person, false, true);

        // Assert
        assertThat(json).contains("\n", "\t");
    }

    @Test
    void parseShouldHandlePolymorphicObjectWhenAutoTypeIsEnabled() {
        // Arrange
        Zoo zoo = new Zoo();
        zoo.name = "My Zoo";
        zoo.setAnimal(new Dog());
        String json = parser.toJsonString(zoo, false, false);

        // Act
        Zoo deserializedZoo = parser.parse(json, Zoo.class, false);

        // Assert
        assertThat(deserializedZoo).isNotNull();
        assertThat(deserializedZoo.getAnimal()).isNotNull().isInstanceOf(Dog.class);
        assertThat(((Dog) deserializedZoo.getAnimal()).breed).isEqualTo("Golden Retriever");
    }

    @Test
    void parseShouldIgnoreAutoTypeWhenSpecified() {
        // Arrange
        String jsonWithAutoType = "{\"@type\":\"" + Person.class.getName() + "\",\"age\":45,\"name\":\"Gendo\"}";
        Person expectedPerson = new Person();
        expectedPerson.setName("Gendo");
        expectedPerson.setAge(45);

        // Act
        Person actualPerson = parser.parse(jsonWithAutoType, Person.class, true);

        // Assert
        assertThat(actualPerson).isEqualTo(expectedPerson);
    }
}
