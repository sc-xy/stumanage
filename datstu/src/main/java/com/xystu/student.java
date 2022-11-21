package com.xystu;

import java.io.IOException;
import java.io.RandomAccessFile;

public class student {
    private String id;
    private String name;
    private String sex;
    private int age = 0;
    private String dep;

    public student() {
    }

    public student(String id, String name, String sex, int age, String dep) {
        setId(id);
        setName(name);
        setSex(sex);
        setAge(age);
        setDep(dep);
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getSex() {
        return this.sex;
    }

    public int getAge() {
        return this.age;
    }

    public String getDep() {
        return this.dep;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setDep(String dep) {
        this.dep = dep;
    }

    public long writeFile(RandomAccessFile raf) throws IOException {
        raf.seek(raf.length());
        long offset = raf.getFilePointer();
        raf.writeUTF(this.id);
        raf.writeUTF(this.name);
        raf.writeUTF(this.sex);
        raf.writeInt(this.age);
        raf.writeUTF(this.dep);
        return offset;
    }

    public void readFile(RandomAccessFile raf, long offset) throws IOException {
        raf.seek(offset);
        readId_File(raf);
        readName_FIle(raf);
        readSex_File(raf);
        readAge_File(raf);
        readDep_File(raf);
    }

    public void readId_File(RandomAccessFile raf) throws IOException {
        id = raf.readUTF();
    }

    public void readName_FIle(RandomAccessFile raf) throws IOException {
        this.name = raf.readUTF();
    }

    public void readSex_File(RandomAccessFile raf) throws IOException {
        this.sex = raf.readUTF();
    }

    public void readAge_File(RandomAccessFile raf) throws IOException {
        this.age = raf.readInt();
    }

    public void readDep_File(RandomAccessFile raf) throws IOException {
        this.dep = raf.readUTF();
    }

    public void show() {
        System.out.printf("学号：%-6s; 姓名：%3s; 性别：%s; 年龄：%-2s; 学院：%-8s\n", this.id, this.name, this.sex, this.age,
                this.dep);
    }

    public void show(String str) {
        System.out.print(str);
        System.out.printf("学号：%-6s; 姓名：%3s; 性别：%s; 年龄：%-2s; 学院：%-8s\n", this.id, this.name, this.sex, this.age,
                this.dep);
    }

}