package com.xystu;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import net.sf.json.JSONObject;

public class SQLCon {
    private Connection connection;
    private PreparedStatement pst;
    private final String DB_URL;
    private final String name;
    private final String passwd;

    // 本地缓存，缓存数据库中已存在学生信息和不存在的学号信息
    private final ArrayList<student> redis = new ArrayList<>();
    private final ArrayList<String> idNotExist = new ArrayList<>();
    // 输入
    public final Scanner input;

    final String selectSQL = "select * from student where id = ?";
    final String insertSQL = "insert into student(id, stu_name, sex, age, dep, is_delete) values(?, ?, ?, ?, ?, ?)";
    final String updateSQL = "update student set stu_name = ?, sex = ?, age = ?, dep = ?, is_delete = ? where id = ?";
    final String deleteSQL = "delete from student where id = ?";

    public static void main(String[] args) throws SQLException {
        SQLCon a = new SQLCon();
        int b = 0;
        while (b != 6) {
            a.showMenu();
            b = a.input.nextInt();
            switch (b) {
                case 1:
                    a.insertStudent();
                    break;
                case 2:
                    a.deleteStudent();
                    break;
                case 3:
                    a.selectStudent();
                    break;
                case 4:
                    a.updateStudent();
                    break;
                case 5:
                    a.recoverStudent();
                    break;
                default: {
                }
            }
            a.closeAll();
        }
        a.save();
    }

    public SQLCon() {
        // 配置信息
        JSONObject jsonObject = JSONObject.fromObject(readFileByLines("test.json"));
        String ip = jsonObject.getString("ip");
        String port = jsonObject.getString("port");
        String database = jsonObject.getString("database");

        // 驱动连接url
        DB_URL = "jdbc:mysql://" + ip + ":" + port + "/" + database;
        name = jsonObject.getString("name");
        passwd = jsonObject.getString("password");

        // 输入配置
        input = new Scanner(System.in);
    }

    public void insertStudent() throws SQLException {
        // 添加学生信息
        assert connection != null;
        String id = inputId();

        // 本地缓存查询
        int index = redis.indexOf(new student(id, "", "", 0, "", (byte) 0));

        if (index == -1) {
            // 如果本地缓存里没有该学生信息，就检索数据库
            ResultSet res = checkIdSQL(id);

            if (res.next()) {
                // 如果数据库里有该学生信息
                student tmp = stuFromRes(res);

                // 释放资源
                res.close();
                closeAll();

                // 添加到缓存区
                redis.add(tmp);

                // 处理该学生是否删除
                insertStuExist(tmp);
            } else {
                res.close();
                closeAll();
                // 数据库中不存在学生信息，直接插入
                // 输入学生信息
                student tmp = inputStu(id);

                // 插入学生信息，添加到缓存区
                insertStu(tmp);
                redis.add(tmp);
                idNotExist.remove(id);
                System.out.print("添加成功： ");
                tmp.show();
            }
        } else {
            // 如果缓存区存在该学生信息
            student tmp = redis.get(index);

            // 处理该学生是否删除
            insertStuExist(tmp);
        }
    }

    public void deleteStudent() throws SQLException {
        // 删除学生信息（修改）
        assert connection != null;
        String id = inputId();

        // 在已知不在数据库的缓存中查询
        if (idNotExist.contains(id)) {
            // 已知不存在
            System.out.println("该学生不存在");
            return;
        }

        // 本地缓存查询
        int index = redis.indexOf(new student(id, "", "", 0, "", (byte) 0));
        if (index == -1) {
            // 学生不在缓存区，SQL查询，存在且删除、不存在都返回，存在未删除则删除，并添加到缓存
            ResultSet res = checkIdSQL(id);
            if (res.next()) {
                // 如果数据库中存在学生信息
                student tmp = stuFromRes(res);

                // 添加到缓存区
                redis.add(tmp);

                // 释放资源
                res.close();
                closeAll();

                if (tmp.getIsDelete() == 1) {
                    // 学生存在且被已经被删除，返回
                    System.out.println("该学生不存在");
                } else {
                    // 学生存在未被删除，更新信息
                    tmp.setIsDelete((byte) 1);
                    updateStu(tmp);
                    System.out.print("删除成功： ");
                    tmp.show();
                }
            } else {
                // 释放资源
                res.close();
                closeAll();

                // 如果学生不存在，将学号添加到缓存区
                System.out.println("该学生不存在");
                idNotExist.add(id);
            }
        } else {
            // 如果学生在缓存区
            student tmp = redis.get(index);
            if (tmp.getIsDelete() == 1) {
                // 学生在缓存区已被删除
                System.out.println("该学生不存在");
            } else {
                // 学生在缓存区未被删除，更新信息
                tmp.setIsDelete((byte) 1);
                updateStu(tmp);
                System.out.print("删除成功： ");
                tmp.show();
            }
        }
    }

    public void selectStudent() throws SQLException {
        // 查找学生
        assert connection != null;
        String id = inputId();
        // 本地查询
        if (idNotExist.contains(id)) {
            // 如果学号已知不存在
            System.out.println("该学生不存在");
            return;
        }

        int index = redis.indexOf(new student(id, "", "", 0, "", (byte) 0));
        if (index == -1) {
            // 本地缓存中不存在，SQL查询
            ResultSet res = checkIdSQL(id);
            if (res.next()) {
                // 如果数据库中存在，添加到缓存区
                student tmp = stuFromRes(res);
                redis.add(tmp);

                // 释放资源
                res.close();
                closeAll();

                if (tmp.getIsDelete() == 0) {
                    // 如果学生未被删除，输出
                    tmp.show();
                } else {
                    // 学生已被删除
                    System.out.println("该学生不存在");
                }
            } else {
                // 释放资源
                res.close();
                closeAll();

                System.out.println("该学生不存在");
                idNotExist.add(id);
            }
        } else {
            // 本地缓存中存在，判断是否被删除
            student tmp = redis.get(index);
            if (tmp.getIsDelete() == 0) {
                // 学生未被删除，输出
                tmp.show();
            } else {
                // 学生已删除
                System.out.println("该学生不存在");
            }
        }
    }

    public void updateStudent() throws SQLException {
        // 修改数据
        String id = inputId();
        if (idNotExist.contains(id)) {
            // 已知学号不存在
            System.out.println("该学生不存在");
            return;
        }
        // 本地查询
        int index = redis.indexOf(new student(id, "", "", 0, "", (byte) 0));
        if (index == -1) {
            // 本地缓存中不存在，查询数据库
            ResultSet res = checkIdSQL(id);

            if (res.next()) {
                // 如果数据库中存在，添加到缓存区
                student tmp = stuFromRes(res);
                redis.add(tmp);

                // 释放资源
                res.close();
                closeAll();

                if (tmp.getIsDelete() == 1) {
                    // 如果该学生已经删除
                    System.out.println("该学生不存在");
                } else {
                    // 该学生未被删除，输入信息并更新
                    tmp.copyStu(inputStu(tmp.getId()));
                    updateStu(tmp);
                    System.out.print("更新完成");
                    tmp.show();
                }
            } else {

                // 释放资源
                res.close();
                closeAll();

                // 数据库中不存在，添加到缓存
                System.out.println("该学生不存在");
                idNotExist.add(id);
            }
        } else {
            // 本地缓存存在
            student tmp = redis.get(index);
            if (tmp.getIsDelete() == 0) {
                // 学生未被删除
                tmp.copyStu(inputStu(tmp.getId()));
                updateStu(tmp);
                System.out.println("更新完成： ");
                tmp.show();
            } else {
                // 学生被删除
                System.out.println("该学生不存在");
            }
        }
    }

    public void recoverStudent() {
        // 恢复学生，因为本地缓存中一定存在所有已删除学生，故从本地缓存中读取即可
        String id = inputId();
        if (idNotExist.contains(id)) {
            // 如果已知不存在
            System.out.println("该学生不存在");
            return;
        }
        int index = redis.indexOf(new student(id, "", "", 0, "", (byte) 0));
        if (index == -1) {
            // 本地缓存中不存在
            System.out.println("该学生无法执行此操作");
        } else {
            student tmp = redis.get(index);
            if (tmp.getIsDelete() == 0) {
                // 如果学生未被删除
                System.out.println("该学生无法执行此操作");
            } else {
                tmp.setIsDelete((byte) 0);
                System.out.println("已恢复学生信息");
                updateStu(tmp);
            }
        }
    }

    public ResultSet checkIdSQL(String id) {
        // 在数据库里以学号查找并返回结果
        try {
            getConn();
            getPst(selectSQL);
            pst.setString(1, id);
            return pst.executeQuery();
        } catch (SQLException e) {
            System.out.println("连接失败");
            e.printStackTrace();
            return null;
        }
    }

    public student inputStu(String id) {
        // 输入学生信息，默认学生未被删除
        System.out.print("请输入姓名：");
        String tmpName = input.next();
        System.out.print("请输入性别：");
        String tmpSex = input.next();
        System.out.print("请输入年龄：");
        int tmpAge = input.nextInt();
        System.out.print("请输入学院：");
        String tmpDep = input.next();
        byte tmpFlag = 0;

        return new student(id, tmpName, tmpSex, tmpAge, tmpDep, tmpFlag);
    }

    public student stuFromRes(ResultSet res) {
        // 从结果中返回学生信息
        try {
            String tmpId = res.getString(1);
            String tmpName = res.getString(2);
            String tmpSex = res.getString(3);
            int tmpAge = res.getInt(4);
            String tmpDep = res.getString(5);
            byte tmpFlag = res.getByte(6);
            return new student(tmpId, tmpName, tmpSex, tmpAge, tmpDep, tmpFlag);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("连接异常");
            return null;
        }
    }

    public void deleteStu(String id) {
        try {
            // 建立连接
            getConn();
            getPst(deleteSQL);

            // 处理SQL语句
            pst.setString(1, id);

            // 执行语句
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("连接失败");
        }
    }

    public void showMenu() {
        System.out.println("请输入选择：");
        System.out.println("1. 添加学生信息");
        System.out.println("2. 删除学生信息");
        System.out.println("3. 查找学生信息");
        System.out.println("4. 修改学生信息");
        System.out.println("5. 恢复学生信息");
        System.out.print("6. 退出\n请输入：");
    }

    public void updateStu(String id, String name, String sex, int age, String dep, byte isdelete) {
        // 依照学号修改
        try {
            // 建立连接
            getConn();
            getPst(updateSQL);

            // 处理SQL语句
            pst.setString(6, id);
            pst.setString(1, name);
            pst.setString(2, sex);
            pst.setInt(3, age);
            pst.setString(4, dep);
            pst.setByte(5, isdelete);

            // 执行语句
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("连接失败");
        }
    }

    public void updateStu(student stu) {
        updateStu(stu.getId(), stu.getName(), stu.getSex(), stu.getAge(), stu.getDep(), stu.getIsDelete());
    }

    public void insertStuExist(student tmp) {
        // 处理添加学生是否存在问题
        if (tmp.getIsDelete() == 0) {
            // 学号存在未被删除，添加失败
            System.out.println("该学号已经存在");
        } else {
            // 学号存在已删除，更新信息
            // 输入学生信息
            student tmpStu = inputStu(tmp.getId());

            // 修改学生信息，添加到缓存区
            updateStu(tmpStu);

            // 更新缓存区学生信息
            tmp.copyStu(tmpStu);

            // 添加成功
            System.out.println("添加成功： ");
            tmp.show();
        }
    }

    public void insertStu(String id, String name, String sex, int age, String dep, byte isdelete) {
        // 添加学生
        try {
            // 建立连接
            getConn();
            getPst(insertSQL);

            // 处理SQL语句
            pst.setString(1, id);
            pst.setString(2, name);
            pst.setString(3, sex);
            pst.setInt(4, age);
            pst.setString(5, dep);
            pst.setByte(6, isdelete);

            // 执行语句
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("连接失败");
        }
    }

    public void insertStu(student stu) {
        insertStu(stu.getId(), stu.getName(), stu.getSex(), stu.getAge(), stu.getDep(), stu.getIsDelete());
    }

    public void getConn() {
        // 加载驱动
        // jdbc4.0 之后 SPI自动加载驱动，不需要显式加载
        try {
            // 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            // System.out.println("驱动加载成功！");
        } catch (ClassNotFoundException e) {
            System.out.println("驱动加载失败！");
            e.printStackTrace();
        }

        // 连接数据库
        try {
            this.connection = DriverManager.getConnection(DB_URL, name, passwd);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("数据库连接失败！");
        }
    }

    protected void closeAll() {
        // 关闭连接，释放资源
        try {
            this.pst.close();
            this.connection.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        } catch (NullPointerException e2) {
            return;
        }
    }

    public void getPst(String sql) {
        try {
            // 预处理SQL
            this.pst = this.connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save() throws SQLException {
        // 释放输入资源，将数据库中已删除未恢复的学生信息删除
        boolean flag = false;
        getConn();
        getPst(deleteSQL);
        for (student o : redis) {
            if (o.getIsDelete() == 1) {
                // 删除所有已删除
                pst.setString(1, o.getId());
                pst.addBatch();
                flag = true;
            }
        }
        if (flag) {
            pst.executeBatch();
        }
        input.close();
    }

    public static String readFileByLines(String fileName) {
        // 将json文件转化成json字符串
        FileInputStream file;
        BufferedReader reader = null;
        InputStreamReader inputFileReader;
        String content = "";
        String tempString;
        try {
            file = new FileInputStream(fileName);
            inputFileReader = new InputStreamReader(file, StandardCharsets.UTF_8);
            reader = new BufferedReader(inputFileReader);
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                content = content.concat(tempString);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return content;
    }

    public String inputId() {
        // 提醒输入Id
        System.out.print("请输入学号：");
        return input.next();
    }

}
