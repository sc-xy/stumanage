package com.xystu;

import java.io.Serializable;

public class student implements Serializable {
    private String id;
    private String name;
    private String sex;
    private int age = 0;
    private String dep;
    private byte isDelete;

    public student() {
    }

    public student(String id, String name, String sex, int age, String dep, byte isDelete) {
        this.setIsDelete(isDelete);
        this.setId(id);
        this.setName(name);
        this.setSex(sex);
        this.setAge(age);
        this.setDep(dep);
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDep(String dep) {
        this.dep = dep;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIsDelete(byte isDelete) {
        this.isDelete = isDelete;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public int getAge() {
        return age;
    }

    public String getDep() {
        return dep;
    }

    public byte getIsDelete() {
        return isDelete;
    }

    public void copyStu(student other) {
        // 从学生类中复制学生信息
        this.setId(other.getId());
        this.setName(other.getName());
        this.setSex(other.getSex());
        this.setAge(other.getAge());
        this.setDep(other.getDep());
        this.setIsDelete(other.getIsDelete());
    }

    public void show() {
        System.out.println("学号：" + id + "  姓名：" + name + "  性别：" + sex + "  年龄：" + age + "  学院：" + dep);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof student) {
            return (this.id).equals(((student) other).id);
        }
        return false;
    }

}
