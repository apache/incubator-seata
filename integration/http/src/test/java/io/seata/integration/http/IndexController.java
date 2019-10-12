package io.seata.integration.http;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author : wangxb
 * @Description: mock springmvc controller,one should be start real service
 */
@Controller
public class IndexController {


    @RequestMapping("/index")
    @ResponseBody
    public String index() {

        return "Hello World!";
    }


    @ResponseBody
    @PostMapping("/testPost")
    public String testPost(@RequestBody Person person) {

        return person.toString();
    }


    public static class Person {
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
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }

}
