package com.gitee.linzl;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Bootstrap {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext classPathXmlApplicationContext =
                new ClassPathXmlApplicationContext("demo.xml");
        Person person = classPathXmlApplicationContext.getBean(Person.class);
        System.out.println(person);
    }
}