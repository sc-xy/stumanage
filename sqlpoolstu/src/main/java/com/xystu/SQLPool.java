package com.xystu;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.sf.json.JSONObject;

public class SQLPool {
    private DataSource ds;

    // 数据库中是否查询到ID
    private boolean checkFlag;

    final private Scanner input;
    final private String DB_URL;
    final private String name;
    final private String passwd;

    // 本地缓存，缓存数据库中已存在学生信息和不存在的学号信息
    private final ArrayList<student> redis = new ArrayList<>();
    private final ArrayList<String> idNotExist = new ArrayList<>();

    // 数据库配置信息
    final String selectSQL = "select * from student where id = ?";
    final String insertSQL = "insert into student(id, stu_name, sex, age, dep, is_delete) values(?, ?, ?, ?, ?, ?)";
    final String updateSQL = "update student set stu_name = ?, sex = ?, age = ?, dep = ?, is_delete = ? where id = ?";
    final String deleteSQL = "delete from student where id = ?";

    public static void main(String[] args) {
        SQLPool a = new SQLPool();
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
        }
        a.save();
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

    public void updateStudent() {
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
            student tmp = checkIdSQL(id);

            if (checkFlag) {
                // 如果数据库中存在，添加到缓存区
                redis.add(tmp);

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
                // 数据库中不存在，添加到缓存
                System.out.println("该学生不存在");
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

    public void selectStudent() {
        // 查找学生
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
            student tmp = checkIdSQL(id);
            if (checkFlag) {
                // 如果数据库中存在，添加到缓
                redis.add(tmp);

                if (tmp.getIsDelete() == 0) {
                    // 如果学生未被删除，输出
                    tmp.show();
                } else {
                    // 学生已被删除
                    System.out.println("该学生不存在");
                }
            } else {
                System.out.println("该学生不存在");
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

    public void deleteStudent() {
        // 删除学生信息（修改）
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
            student tmp = checkIdSQL(id);
            if (this.checkFlag) {
                // 添加到缓存区
                redis.add(tmp);

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
                // 学生不存在
                System.out.println("该学生不存在");
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

    public void insertStudent() {
        // 添加学生信息
        String id = inputId();

        // 本地缓存查询
        int index = redis.indexOf(new student(id, "", "", 0, "", (byte) 0));

        if (index == -1) {
            // 如果本地缓存存在
            student tmp = checkIdSQL(id);
            if (this.checkFlag) {
                // 数据库中有学生信息，添加到缓存区
                redis.add(tmp);

                // 处理该学生是否删除
                insertStuExist(tmp);
            } else {
                // 数据库中不存在学生信息，直接插入
                // 输入学生信息
                student tmpstu = inputStu(id);

                // 插入学生信息，添加到缓存区
                insertStu(tmpstu);
                redis.add(tmpstu);
                idNotExist.remove(id);
                System.out.print("添加成功： ");
                tmpstu.show();
            }
        } else {
            // 如果缓存区存在该学生信息
            student tmp = redis.get(index);

            // 处理该学生是否删除
            insertStuExist(tmp);
        }

    }

    public void insertStu(String id, String name, String sex, int age, String dep, byte isdelete) {
        // 添加学生
        try (Connection conn = ds.getConnection();) {
            // 处理SQL语句
            PreparedStatement pst = conn.prepareStatement(insertSQL);
            pst.setString(1, id);
            pst.setString(2, name);
            pst.setString(3, sex);
            pst.setInt(4, age);
            pst.setString(5, dep);
            pst.setByte(6, isdelete);

            // 执行语句
            pst.executeUpdate();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("连接失败");
        }
    }

    public void insertStu(student stu) {
        insertStu(stu.getId(), stu.getName(), stu.getSex(), stu.getAge(), stu.getDep(), stu.getIsDelete());
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

    public void updateStu(String id, String name, String sex, int age, String dep, byte isdelete) {
        // 依照学号修改
        try (Connection conn = ds.getConnection();) {
            // 处理SQL语句
            PreparedStatement pst = conn.prepareStatement(updateSQL);
            pst.setString(6, id);
            pst.setString(1, name);
            pst.setString(2, sex);
            pst.setInt(3, age);
            pst.setString(4, dep);
            pst.setByte(5, isdelete);

            // 执行语句
            pst.executeUpdate();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("连接失败");
        }
    }

    public void updateStu(student stu) {
        updateStu(stu.getId(), stu.getName(), stu.getSex(), stu.getAge(), stu.getDep(), stu.getIsDelete());
    }

    public void save() {
        // 释放输入资源，将数据库中已删除未恢复的学生信息删除
        boolean flag = false;
        try (Connection conn = ds.getConnection()) {
            PreparedStatement pst = conn.prepareStatement(deleteSQL);
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
        } catch (Exception e) {
            System.out.println("保存错误，删除学生失效");
        }
        input.close();
    }

    public SQLPool() throws ExceptionInInitializerError {
        // 数据库配置信息
        JSONObject jsonObject = JSONObject.fromObject(readFileByLines("test.json"));
        String ip = jsonObject.getString("ip");
        String port = jsonObject.getString("port");
        String database = jsonObject.getString("database");

        // 驱动连接url
        DB_URL = "jdbc:mysql://" + ip + ":" + port + "/" + database;
        name = jsonObject.getString("name");
        passwd = jsonObject.getString("password");

        // 配置
        input = new Scanner(System.in);

        // 连接池配置
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(name);
        config.setPassword(passwd);
        config.addDataSourceProperty("connectionTimeout", "1000"); // 连接超时：1秒
        config.addDataSourceProperty("idleTimeout", "60000"); // 空闲超时：60秒
        config.addDataSourceProperty("maximumPoolSize", "20"); // 最大连接数：20

        ds = new HikariDataSource(config);
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
        System.out.print(new student(id, tmpName, tmpSex, tmpAge, tmpDep, tmpFlag));
        return new student(id, tmpName, tmpSex, tmpAge, tmpDep, tmpFlag);
    }

    public String inputId() {
        // 提醒输入Id
        System.out.print("请输入学号：");
        return input.next();
    }

    public student checkIdSQL(String id) {
        // 在数据库里以学号查找并返回结果
        try (Connection conn = ds.getConnection()) {
            // 处理SQL语句
            PreparedStatement pst = conn.prepareStatement(selectSQL);
            pst.setString(1, id);
            // 查询
            ResultSet res = pst.executeQuery();

            if (res.next()) {
                // 数据库中有对应学生信息
                this.checkFlag = true;
                student tmpstu = stuFromRes(res);
                closeAll(res, pst);
                return tmpstu;
            } else {
                // 数据库中没有对应学生信息
                this.checkFlag = false;
                // 只有查询到没有对应id时才添加
                idNotExist.add(id);
                closeAll(res, pst);
                return new student();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("连接发生错误");
            return null;
        }
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

    public static void closeAll(ResultSet a, PreparedStatement b) {
        try {
            a.close();
            b.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("未知错误");
        }
    }

}
