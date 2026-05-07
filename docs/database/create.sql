CREATE TABLE users (
    id CHAR(9) NOT NULL,
    name VARCHAR(20) NOT NULL,
    sex BOOLEAN NOT NULL,
    type ENUM('student', 'teacher', 'admin') NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone CHAR(11) NOT NULL DEFAULT '00000000000',
    email VARCHAR(254),
    PRIMARY KEY (id)
);

INSERT INTO users (id, name, sex, type, password, phone,email) 
VALUES ('213230001', '张三', TRUE, 'student', '123456', '12345678910', NULL),
('213230002', '李四', False, 'student', '123456', '12345678910', NULL),
('101010001', '李刚', TRUE, 'teacher', '123456', '12345678910', NULL),
('101010002', '王红', False, 'teacher', '123456', '12345678910', NULL),
('000000001', '甲', TRUE, 'admin', '123456', '12345678910', NULL),
('000000002', '乙', FALSE, 'admin', '123456', '12345678910', NULL);

CREATE TABLE student (
    id char(9) NOT NULL,
    sno char(8) NOT NULL,
    enteryear int NOT NULL,
    major varchar(50) NOT NULL,
    department varchar(30) NOT NULL,
    PRIMARY KEY (sno),
    FOREIGN KEY (id) REFERENCES users(id)
);

CREATE TABLE teacher (
    id char(9) NOT NULL,
    eid char(8) NOT NULL,
    enteryear int NOT NULL,
    title ENUM('professor', 'associate_professor', 'lecture') NOT NULL,
    department varchar(30) NOT NULL,
    PRIMARY KEY (eid),
    FOREIGN KEY (id) REFERENCES users(id)
);

INSERT INTO student (id, sno, enteryear, major, department) 
VALUES 
    ('213230001', '09023001', 2023, '计算机科学与技术', '计算机学院'),
    ('213230002', '71123002', 2023, '软件工程', '计算机学院');

INSERT INTO teacher (id, eid, enteryear, title, department) 
VALUES 
    ('101010001', '09T10001', 2010, 'professor', '计算机学院'),
    ('101010002', '71T15002', 2015, 'associate_professor', '软件学院');

CREATE TABLE course(
    cno char(8),
    cname varchar(50),
    credit decimal(2,1),
    PRIMARY KEY (cno)
);

INSERT INTO course (cno, cname, credit)
VALUES
    ('BJSL0001', '计算机组成原理', 4.0),
    ('BJSL0002', '数据结构', 4.0),
    ('BJSL0003', '操作系统', 4.0),
    ('BJSL0004', '计算机网络', 3.0),
    ('B07M0001', '高等数学', 5.0),
    ('B07M0002', '线性代数', 3.5),
    ('B09D0001', '数据库原理', 3.0),
    ('B71C0001', '编译原理', 4.0),
    ('B71S0001', '软件工程', 3.0),
    ('BJSL0012', 'C++程序设计', 4.5),
    ('B10M0001', '大学物理', 3.0),
    ('B58I0001', '人工智能', 3.5),
    ('B09A0001', '算法设计', 3.0),
    ('B17M0001', '大学英语', 2.0);

CREATE TABLE class(
    cno char(8),
    eid char(8),
    PRIMARY KEY (cno, eid),
    FOREIGN KEY (eid) REFERENCES teacher(eid)
);

CREATE TABLE sc(
    sno char(8),
    cno char(8),
    eid char(8),
    grade tinyint unsigned,
    PRIMARY KEY (sno,cno,eid),
    FOREIGN KEY (sno) REFERENCES student(sno),
    FOREIGN KEY (cno,eid) REFERENCES class(cno,eid)
);