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
    major varchar(20) NOT NULL,
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
    cname varchar(20),
    credit decimal(2,1),
    PRIMARY KEY (cno)
);

CREATE TABLE class(
    cno char(8),
    eid char(8),
    PRIMARY KEY (cno, eid),
    FOREIGN KEY (eid) REFERENCES teacher(eid)
);

CREATE TABLE SC(
    sno char(8),
    cno char(8),
    eid char(8),
    grade tinyint unsigned,
    PRIMARY KEY (sno,cno,eid),
    FOREIGN KEY (sno) REFERENCES student(sno),
    FOREIGN KEY (cno,eid) REFERENCES class(cno,eid)
);

CREATE TABLE question_bank (
    id varchar(32) NOT NULL,
    stem varchar(512) NOT NULL,
    answer_key varchar(64) NOT NULL,
    difficulty double NOT NULL,
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id)
);

CREATE TABLE question_option (
    id bigint NOT NULL AUTO_INCREMENT,
    question_id varchar(32) NOT NULL,
    option_text varchar(255) NOT NULL,
    option_order int NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (question_id) REFERENCES question_bank(id)
);

INSERT INTO question_bank (id, stem, answer_key, difficulty, active) VALUES
('Q001', '6 + 7 等于多少？', '13', -2.2, true),
('Q002', '9 * 8 等于多少？', '72', -1.7, true),
('Q003', '求解方程：2x + 5 = 19。', '7', -1.1, true),
('Q004', 'x^2 的导数是：', '2x', -0.6, true),
('Q005', '以下哪种数据结构是 FIFO？', '队列', -0.2, true),
('Q006', 'SQL 中用于过滤行的关键字是：', 'WHERE', 0.1, true),
('Q007', '二分查找的平均时间复杂度是：', 'O(log n)', 0.3, true),
('Q008', '若 p -> q 且 p 为真，则 q 为：', '真', 0.7, true),
('Q009', '第一范式（1NF）主要要求：', '列值原子性', 1.0, true),
('Q010', 'JVM 的含义是：', 'Java 虚拟机', 1.3, true),
('Q011', 'REST 中幂等更新常用的方法是：', 'PUT', 1.4, true),
('Q012', '对 2x 求积分是：', 'x^2 + C', 1.6, true),
('Q013', '梯度下降主要用于：', '最小化损失', 1.9, true),
('Q014', 'CAP 理论在网络分区时通常权衡：', '一致性或可用性', 2.2, true),
('Q015', '在 IRT/CAT 中，theta 通常表示：', '潜在能力水平', 2.5, true);

INSERT INTO question_option (question_id, option_text, option_order) VALUES
('Q001', '11', 1),
('Q001', '12', 2),
('Q001', '13', 3),
('Q001', '14', 4),
('Q002', '62', 1),
('Q002', '72', 2),
('Q002', '81', 3),
('Q002', '64', 4),
('Q003', '6', 1),
('Q003', '7', 2),
('Q003', '8', 3),
('Q003', '9', 4),
('Q004', 'x', 1),
('Q004', '2x', 2),
('Q004', 'x^2', 3),
('Q004', '2', 4),
('Q005', '栈', 1),
('Q005', '队列', 2),
('Q005', '树', 3),
('Q005', '堆', 4),
('Q006', 'ORDER', 1),
('Q006', 'WHERE', 2),
('Q006', 'GROUP', 3),
('Q006', 'INDEX', 4),
('Q007', 'O(n)', 1),
('Q007', 'O(log n)', 2),
('Q007', 'O(n log n)', 3),
('Q007', 'O(1)', 4),
('Q008', '真', 1),
('Q008', '假', 2),
('Q008', '未知', 3),
('Q008', '真且假', 4),
('Q009', '不允许空值', 1),
('Q009', '列值原子性', 2),
('Q009', '不允许重复行', 3),
('Q009', '必须有外键', 4),
('Q010', 'Java 可信模型', 1),
('Q010', 'Java 虚拟机', 2),
('Q010', '联合向量内存', 3),
('Q010', 'Java 供应商模块', 4),
('Q011', 'POST', 1),
('Q011', 'PATCH', 2),
('Q011', 'PUT', 3),
('Q011', 'CONNECT', 4),
('Q012', 'x^2 + C', 1),
('Q012', '2x + C', 2),
('Q012', 'x + C', 3),
('Q012', 'x^3 + C', 4),
('Q013', '最大化方差', 1),
('Q013', '最小化损失', 2),
('Q013', '编码标签', 3),
('Q013', '排序数组', 4),
('Q014', '一致性或可用性', 1),
('Q014', '延迟或吞吐', 2),
('Q014', '内存或 CPU', 3),
('Q014', '存储或网络', 4),
('Q015', '题目数量', 1),
('Q015', '潜在能力水平', 2),
('Q015', '考试时长', 3),
('Q015', '难度方差', 4);

CREATE TABLE exam_paper (
    id varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id)
);

CREATE TABLE exam_definition (
    id varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    description varchar(255),
    max_questions int NOT NULL,
    active boolean NOT NULL DEFAULT true,
    is_default boolean NOT NULL DEFAULT false,
    paper_id varchar(32) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (paper_id) REFERENCES exam_paper(id)
);

CREATE TABLE exam_paper_question (
    paper_id varchar(32) NOT NULL,
    question_id varchar(32) NOT NULL,
    display_order int NOT NULL,
    PRIMARY KEY (paper_id, question_id),
    FOREIGN KEY (paper_id) REFERENCES exam_paper(id),
    FOREIGN KEY (question_id) REFERENCES question_bank(id)
);

INSERT INTO exam_paper (id, name, active) VALUES
('PAPER_DEFAULT', '标准题库', true),
('PAPER_BASIC', '基础题库', true);

INSERT INTO exam_definition (id, name, description, max_questions, active, is_default, paper_id) VALUES
('EXAM_DEFAULT', '标准考试', '覆盖全部题目', 10, true, true, 'PAPER_DEFAULT'),
('EXAM_BASIC', '基础考试', '基础题目集合', 5, true, false, 'PAPER_BASIC');

INSERT INTO exam_paper_question (paper_id, question_id, display_order) VALUES
('PAPER_DEFAULT', 'Q001', 1),
('PAPER_DEFAULT', 'Q002', 2),
('PAPER_DEFAULT', 'Q003', 3),
('PAPER_DEFAULT', 'Q004', 4),
('PAPER_DEFAULT', 'Q005', 5),
('PAPER_DEFAULT', 'Q006', 6),
('PAPER_DEFAULT', 'Q007', 7),
('PAPER_DEFAULT', 'Q008', 8),
('PAPER_DEFAULT', 'Q009', 9),
('PAPER_DEFAULT', 'Q010', 10),
('PAPER_DEFAULT', 'Q011', 11),
('PAPER_DEFAULT', 'Q012', 12),
('PAPER_DEFAULT', 'Q013', 13),
('PAPER_DEFAULT', 'Q014', 14),
('PAPER_DEFAULT', 'Q015', 15),
('PAPER_BASIC', 'Q001', 1),
('PAPER_BASIC', 'Q002', 2),
('PAPER_BASIC', 'Q003', 3),
('PAPER_BASIC', 'Q004', 4),
('PAPER_BASIC', 'Q005', 5);