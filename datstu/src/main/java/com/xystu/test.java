package com.xystu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.util.Scanner;

public class test {
    public static void main(String[] args)
            throws FileNotFoundException, ClassNotFoundException, IOException, EOFException {
        studentManage stuMan = new studentManage();
        int a = 0;
        Scanner input = new Scanner(System.in);
        while (a != 7) {
            showMenu();
            a = input.nextInt();
            System.out.println();
            if (a == 0)
                stuMan.disPlayAll_Sudo();
            if (a == 1)
                stuMan.disPlayAll();
            if (a == 2)
                stuMan.addStudent(input);
            if (a == 3)
                stuMan.deleteStudent(input);
            if (a == 4)
                stuMan.searchStudent(input);
            if (a == 5)
                stuMan.updateStudent(input);
            if (a == 6)
                stuMan.recover(input);
        }
        input.close();
        stuMan.save();
    }

    public static void showMenu() {
        System.out.println();
        System.out.println("请输入选项: ");
        System.out.println("0. 显示所有数据");
        System.out.println("1. 显示所有学生");
        System.out.println("2. 增加学生");
        System.out.println("3. 删除学生");
        System.out.println("4. 查找学生");
        System.out.println("5. 更新学生信号");
        System.out.println("6. 恢复数据");
        System.out.println("7. 退出");
    }

}
