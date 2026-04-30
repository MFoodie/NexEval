    CREATE TABLE IF NOT EXISTS question_bank (
        id varchar(32) NOT NULL,
        stem varchar(512) NOT NULL,
        answer_key varchar(64) NOT NULL,
        difficulty double NOT NULL,
        active boolean NOT NULL DEFAULT true,
        PRIMARY KEY (id)
    );

    CREATE TABLE IF NOT EXISTS question_option (
        id bigint NOT NULL AUTO_INCREMENT,
        question_id varchar(32) NOT NULL,
        option_text varchar(255) NOT NULL,
        option_order int NOT NULL,
        PRIMARY KEY (id),
        FOREIGN KEY (question_id) REFERENCES question_bank(id)
    );

    INSERT IGNORE INTO question_bank (id, stem, answer_key, difficulty, active) VALUES
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

    INSERT IGNORE INTO question_option (question_id, option_text, option_order) VALUES
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

    CREATE TABLE IF NOT EXISTS exam_paper (
        id varchar(32) NOT NULL,
        name varchar(64) NOT NULL,
        active boolean NOT NULL DEFAULT true,
        PRIMARY KEY (id)
    );

    CREATE TABLE IF NOT EXISTS exam_definition (
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

    CREATE TABLE IF NOT EXISTS exam_paper_question (
        paper_id varchar(32) NOT NULL,
        question_id varchar(32) NOT NULL,
        display_order int NOT NULL,
        PRIMARY KEY (paper_id, question_id),
        FOREIGN KEY (paper_id) REFERENCES exam_paper(id),
        FOREIGN KEY (question_id) REFERENCES question_bank(id)
    );

    INSERT IGNORE INTO exam_paper (id, name, active) VALUES
    ('PAPER_DEFAULT', '标准题库', true),
    ('PAPER_BASIC', '基础题库', true);

    INSERT IGNORE INTO exam_definition (id, name, description, max_questions, active, is_default, paper_id) VALUES
    ('EXAM_DEFAULT', '标准考试', '覆盖全部题目', 10, true, true, 'PAPER_DEFAULT'),
    ('EXAM_BASIC', '基础考试', '基础题目集合', 5, true, false, 'PAPER_BASIC');

    INSERT IGNORE INTO exam_paper_question (paper_id, question_id, display_order) VALUES
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
