package com.xystu;

import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.ObjectInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class studentManage {

    private ArrayList<studentIdx> stuidx = new ArrayList<>();
    private RandomAccessFile raf = new RandomAccessFile("./stu.dat", "rws");
    private ObjectInputStream in;

    public studentManage() throws FileNotFoundException, IOException, ClassNotFoundException, EOFException {
        File file = new File("stu.idx");
        if (!(file.exists())) {
            file.createNewFile(); // 若文件不存在,创建文件
        }
        try {
            in = new ObjectInputStream(new FileInputStream("stu.idx"));
            Object tmp = in.readObject();
            if (tmp instanceof ArrayList<?>) {
                for (Object o : (ArrayList<?>) tmp) {
                    stuidx.add(studentIdx.class.cast(o));
                }
            }
        } catch (Exception e) {
        }
    }

    public void disPlayAll_Sudo() throws IOException {
        if (raf.length() == 0) {
            System.out.println("暂无学生信息!");
            return;
        }
        raf.seek(0);
        try {
            while (true) {
                student tmp = new student();
                tmp.readFile(raf, raf.getFilePointer());
                tmp.show();
            }
        } catch (EOFException e) {
        }
    }

    public void disPlayAll() throws IOException {
        if (stuidx.size() == 0) {
            System.out.println("暂无学生信息!");
            return;
        }
        student stu = new student();
        for (studentIdx tmp : stuidx) {
            stu.readFile(raf, tmp.getOffset());
            stu.show();
        }
    }

    public void deleteStudent(Scanner inp) throws IOException {
        System.out.print("请输入学号：");
        String str1 = inp.next();
        int idx = stuidx.indexOf(new studentIdx(str1, 0));
        if (idx == -1) {
            System.out.println("该学号不存在!");
            return;
        }
        student stu = new student();
        stu.readFile(raf, this.stuidx.get(idx).getOffset());
        stu.show("已删除：");
        this.stuidx.remove(idx);
        return;
    }

    public void addStudent(Scanner inp) throws IOException {
        System.out.print("请输入学号：");
        String str1 = inp.next();
        if (stuidx.indexOf(new studentIdx(str1, 0)) != -1) {
            System.out.println("该学号已存在!");
            return;
        }
        System.out.print("姓名：");
        String str2 = inp.next();
        System.out.print("性别：");
        String str3 = inp.next();
        System.out.print("年龄：");
        int age = inp.nextInt();
        System.out.print("学院：");
        String str4 = inp.next();
        student stuTmp = new student(str1, str2, str3, age, str4);
        stuidx.add(new studentIdx(str1, stuTmp.writeFile(raf)));
        stuTmp.show("已添加：");
    }

    public void searchStudent(Scanner inp) throws IOException {
        System.out.print("请输入学号：");
        String str1 = inp.next();
        int idx = stuidx.indexOf(new studentIdx(str1, 0));
        if (idx == -1) {
            System.out.println("该学号不存在!");
            return;
        }
        student stu = new student();
        stu.readFile(raf, this.stuidx.get(idx).getOffset());
        stu.show();
    }

    public void updateStudent(Scanner inp) throws IOException {
        System.out.print("请输入学号：");
        String tarId = inp.next();
        int idx = stuidx.indexOf(new studentIdx(tarId, 0));
        if (idx == -1) {
            System.out.println("该学号不存在!");
            return;
        }
        System.out.print("学号：");
        String str1 = inp.next();
        System.out.print("姓名：");
        String str2 = inp.next();
        System.out.print("性别：");
        String str3 = inp.next();
        System.out.print("年龄：");
        int age = inp.nextInt();
        System.out.print("学院：");
        String str4 = inp.next();
        if (str1.equals(tarId)) {
            student stu = new student(str1, str2, str3, age, str4);
            stuidx.get(idx).setOffset(stu.writeFile(raf));
            stu.show("修改成功：");
            return;
        } else if (stuidx.indexOf(new studentIdx(str1, 0)) != -1) {
            System.out.println("该学号已存在!");
            return;
        } else {
            student stu = new student(str1, str2, str3, age, str4);
            stu.writeFile(raf);
            stuidx.get(idx).setId(str1);
            stuidx.get(idx).setOffset(stu.writeFile(raf));
            stu.show("修改成功：");
        }
    }

    public void save() throws IOException {
        ArrayList<student> stuList = new ArrayList<>();
        for (studentIdx sidx : stuidx) {
            student stuTmp = new student();
            stuTmp.readFile(raf, sidx.getOffset());
            stuList.add(stuTmp);
        }
        raf.setLength(0);
        raf.seek(0);
        for (int i = 0; i < stuList.size(); i++) {
            stuidx.get(i).setOffset(stuList.get(i).writeFile(raf));
        }

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("stu.idx"));
        out.writeObject(stuidx);
        out.close();
    }

    public void recover(Scanner inp) throws IOException {
        System.out.println("1. 回复所有学生数据\n2. 按学号恢复");
        int b = inp.nextInt();
        if (b == 1)
            recoverAll();
        if (b == 2)
            recoverOne(inp);
    }

    public void recoverAll() throws IOException {
        raf.seek(0);
        try {
            while (true) {
                student tmp = new student();
                long offset = raf.getFilePointer();
                tmp.readFile(raf, offset);
                if (stuidx.indexOf(new studentIdx(tmp.getId(), offset)) == -1) {
                    stuidx.add(new studentIdx(tmp.getId(), offset));
                    tmp.show("已恢复：");
                }
            }
        } catch (EOFException e) {
        }
    }

    public void recoverOne(Scanner inp) throws IOException {
        System.out.print("请输入学号：");
        String str1 = inp.next();
        if (stuidx.indexOf(new studentIdx(str1, 0)) != -1) {
            System.out.println("该学号已存在!");
            return;
        }
        raf.seek(0);
        try {
            while (true) {
                student tmp = new student();
                long offset = raf.getFilePointer();
                tmp.readFile(raf, offset);
                if (tmp.getId().equals(str1)) {
                    stuidx.add(new studentIdx(tmp.getId(), offset));
                    tmp.show("已恢复：");
                    return;
                }
            }
        } catch (EOFException e) {
            System.out.println("该学号无法恢复");
        }

    }

}