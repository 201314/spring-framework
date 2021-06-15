package com.gitee.linzl;

public class Person {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "我的名字是:" + this.name;
    }
}