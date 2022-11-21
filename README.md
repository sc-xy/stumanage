### stumanage
hi，又是我，这是某洋开发的学生信息管理系统，本次采用`maven`多模块框架：
- `datstu`版本：基于本地文件
- `mysqlstu`版本：基于`mysql`:
  - `test.json`存储数据库信息
  - 数据库建表语句：
  ```
    CREATE TABLE `student` (
        `id` char(6) NOT NULL,
        `stu_name` varchar(5) DEFAULT NULL,
        `sex` varchar(3) DEFAULT NULL,
        `age` tinyint(4) DEFAULT NULL,
        `dep` varchar(20) DEFAULT NULL,
        `is_delete` bit(1) NOT NULL DEFAULT b'0',
        PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
  ```
