-- Requires course table from create.sql
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS essay_question_bank (
    id varchar(32) NOT NULL,
    stem varchar(512) NOT NULL,
    standard_answer varchar(1024),
    scoring_rubric varchar(2048),
    points int NOT NULL,
    difficulty decimal(3,1) NOT NULL,
    difficulty_b decimal(4,2) NOT NULL DEFAULT 0.00,
    discrimination_a decimal(4,2) NOT NULL DEFAULT 1.00,
    cno char(8),
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    FOREIGN KEY (cno) REFERENCES course(cno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS essay_exam_paper (
    id varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS essay_exam_definition (
    id varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    description varchar(255),
    max_questions int NOT NULL,
    active boolean NOT NULL DEFAULT true,
    is_default boolean NOT NULL DEFAULT false,
    paper_id varchar(32) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (paper_id) REFERENCES essay_exam_paper(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS essay_exam_paper_question (
    paper_id varchar(32) NOT NULL,
    question_id varchar(32) NOT NULL,
    display_order int NOT NULL,
    PRIMARY KEY (paper_id, question_id),
    FOREIGN KEY (paper_id) REFERENCES essay_exam_paper(id),
    FOREIGN KEY (question_id) REFERENCES essay_question_bank(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO essay_question_bank (id, stem, points, difficulty, cno, active) VALUES
('E001', '【知识点：指令周期】说明取指、译码、执行的基本流程，并指出各阶段主要工作。', 6, 1.5, 'BJSL0001', true),
('E002', '【知识点：缓存】解释局部性原理，并举例说明其对命中率的影响。', 8, 3.0, 'BJSL0001', true),
('E003', '【知识点：流水线】分析数据相关导致的冒险类型，并给出一种缓解方法。', 10, 4.5, 'BJSL0001', true),
('E004', '【知识点：排序】比较归并排序与快速排序的稳定性和平均复杂度。', 6, 1.5, 'BJSL0002', true),
('E005', '【知识点：图】说明 BFS 与 DFS 在最短路径问题中的适用差异。', 8, 3.0, 'BJSL0002', true),
('E006', '【知识点：哈希】描述哈希冲突处理的两种方法及其特点。', 10, 4.5, 'BJSL0002', true),
('E007', '【知识点：进程】描述进程状态转换，并说明阻塞态出现的常见原因。', 6, 1.5, 'BJSL0003', true),
('E008', '【知识点：内存管理】对比分页与分段的优缺点。', 8, 3.0, 'BJSL0003', true),
('E009', '【知识点：同步】给出生产者-消费者问题的基本解法思路。', 10, 4.5, 'BJSL0003', true),
('E010', '【知识点：分层】说明分层模型的优点，并举出两层之间的典型功能。', 6, 1.5, 'BJSL0004', true),
('E011', '【知识点：可靠传输】解释重传与滑动窗口如何协同保证可靠性。', 8, 3.0, 'BJSL0004', true),
('E012', '【知识点：拥塞控制】概述慢启动和拥塞避免的窗口变化规则。', 10, 4.5, 'BJSL0004', true),
('E013', '【知识点：极限】说明极限定义的直观含义，并给出一个简单例子。', 6, 1.5, 'B07M0001', true),
('E014', '【知识点：积分】解释定积分的几何意义并给出应用场景。', 8, 3.0, 'B07M0001', true),
('E015', '【知识点：级数】描述判别级数收敛性的基本思路。', 10, 4.5, 'B07M0001', true),
('E016', '【知识点：线性方程组】说明方程组有解的条件与解的分类。', 6, 1.5, 'B07M0002', true),
('E017', '【知识点：特征值】阐述特征值与特征向量的意义及应用。', 8, 3.0, 'B07M0002', true),
('E018', '【知识点：正交】说明正交向量组的性质及其在计算中的优势。', 10, 4.5, 'B07M0002', true),
('E019', '【知识点：范式】说明 1NF、2NF、3NF 的核心要求与差异。', 6, 1.5, 'B09D0001', true),
('E020', '【知识点：索引】解释 B+ 树索引的结构特点及适用场景。', 8, 3.0, 'B09D0001', true),
('E021', '【知识点：事务】说明 ACID 各特性的含义及作用。', 10, 4.5, 'B09D0001', true),
('E022', '【知识点：词法分析】描述词法分析的输入输出及其在编译流程中的位置。', 6, 1.5, 'B71C0001', true),
('E023', '【知识点：语法分析】说明预测分析与 LR 分析的主要差异。', 8, 3.0, 'B71C0001', true),
('E024', '【知识点：中间代码】举例说明三地址码的表示方式。', 10, 4.5, 'B71C0001', true),
('E025', '【知识点：需求】说明功能需求与非功能需求的区别并举例。', 6, 1.5, 'B71S0001', true),
('E026', '【知识点：测试】阐述单元测试与集成测试的侧重点。', 8, 3.0, 'B71S0001', true),
('E027', '【知识点：过程模型】比较瀑布模型与迭代模型的优缺点。', 10, 4.5, 'B71S0001', true),
('E028', '【知识点：面向对象】说明封装、继承、多态的含义并举例。', 6, 1.5, 'BJSL0012', true),
('E029', '【知识点：内存管理】解释 RAII 思想及其好处。', 8, 3.0, 'BJSL0012', true),
('E030', '【知识点：模板】描述模板的用途与实例化过程。', 10, 4.5, 'BJSL0012', true),
('E031', '【知识点：力学】阐述牛顿三定律并举出一个生活例子。', 6, 1.5, 'B10M0001', true),
('E032', '【知识点：能量】说明动能与势能的转换并给出例子。', 8, 3.0, 'B10M0001', true),
('E033', '【知识点：电学】解释电流、电压、电阻的关系并举例。', 10, 4.5, 'B10M0001', true),
('E034', '【知识点：学习范式】对比监督学习与无监督学习的差异和应用。', 6, 1.5, 'B58I0001', true),
('E035', '【知识点：模型评估】说明准确率与召回率的含义及其权衡。', 8, 3.0, 'B58I0001', true),
('E036', '【知识点：过拟合】描述过拟合产生的原因及常见应对方法。', 10, 4.5, 'B58I0001', true),
('E037', '【知识点：复杂度分析】说明大 O 表示法的意义并举例。', 6, 1.5, 'B09A0001', true),
('E038', '【知识点：动态规划】描述状态设计与转移方程构建思路。', 8, 3.0, 'B09A0001', true),
('E039', '【知识点：贪心】说明选择性质与最优子结构的关系。', 10, 4.5, 'B09A0001', true),
('E040', '【知识点：写作结构】说明引言-正文-结论的基本功能。', 6, 1.5, 'B17M0001', true),
('E041', '【知识点：论证】阐述论点、论据、论证之间的关系。', 8, 3.0, 'B17M0001', true),
('E042', '【知识点：阅读策略】描述精读与略读的使用场景。', 10, 4.5, 'B17M0001', true);

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
        AND table_name = 'essay_question_bank'
        AND column_name = 'standard_answer'
);
SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE essay_question_bank ADD COLUMN standard_answer varchar(1024) DEFAULT NULL AFTER stem',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
        AND table_name = 'essay_question_bank'
        AND column_name = 'scoring_rubric'
);
SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE essay_question_bank ADD COLUMN scoring_rubric varchar(2048) DEFAULT NULL AFTER standard_answer',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO essay_exam_paper (id, name, active) VALUES
('PAPER_DEFAULT', '大题题库', true),
('PAPER_BASIC', '大题基础题库', true);

INSERT IGNORE INTO essay_exam_definition (id, name, description, max_questions, active, is_default, paper_id) VALUES
('EXAM_DEFAULT', '大题标准考试', '覆盖全部大题', 42, true, true, 'PAPER_DEFAULT'),
('EXAM_BASIC', '大题基础考试', '基础大题集合', 14, true, false, 'PAPER_BASIC');

INSERT IGNORE INTO essay_exam_paper_question (paper_id, question_id, display_order) VALUES
('PAPER_DEFAULT', 'E001', 1),
('PAPER_DEFAULT', 'E002', 2),
('PAPER_DEFAULT', 'E003', 3),
('PAPER_DEFAULT', 'E004', 4),
('PAPER_DEFAULT', 'E005', 5),
('PAPER_DEFAULT', 'E006', 6),
('PAPER_DEFAULT', 'E007', 7),
('PAPER_DEFAULT', 'E008', 8),
('PAPER_DEFAULT', 'E009', 9),
('PAPER_DEFAULT', 'E010', 10),
('PAPER_DEFAULT', 'E011', 11),
('PAPER_DEFAULT', 'E012', 12),
('PAPER_DEFAULT', 'E013', 13),
('PAPER_DEFAULT', 'E014', 14),
('PAPER_DEFAULT', 'E015', 15),
('PAPER_DEFAULT', 'E016', 16),
('PAPER_DEFAULT', 'E017', 17),
('PAPER_DEFAULT', 'E018', 18),
('PAPER_DEFAULT', 'E019', 19),
('PAPER_DEFAULT', 'E020', 20),
('PAPER_DEFAULT', 'E021', 21),
('PAPER_DEFAULT', 'E022', 22),
('PAPER_DEFAULT', 'E023', 23),
('PAPER_DEFAULT', 'E024', 24),
('PAPER_DEFAULT', 'E025', 25),
('PAPER_DEFAULT', 'E026', 26),
('PAPER_DEFAULT', 'E027', 27),
('PAPER_DEFAULT', 'E028', 28),
('PAPER_DEFAULT', 'E029', 29),
('PAPER_DEFAULT', 'E030', 30),
('PAPER_DEFAULT', 'E031', 31),
('PAPER_DEFAULT', 'E032', 32),
('PAPER_DEFAULT', 'E033', 33),
('PAPER_DEFAULT', 'E034', 34),
('PAPER_DEFAULT', 'E035', 35),
('PAPER_DEFAULT', 'E036', 36),
('PAPER_DEFAULT', 'E037', 37),
('PAPER_DEFAULT', 'E038', 38),
('PAPER_DEFAULT', 'E039', 39),
('PAPER_DEFAULT', 'E040', 40),
('PAPER_DEFAULT', 'E041', 41),
('PAPER_DEFAULT', 'E042', 42),
('PAPER_BASIC', 'E001', 1),
('PAPER_BASIC', 'E004', 2),
('PAPER_BASIC', 'E007', 3),
('PAPER_BASIC', 'E010', 4),
('PAPER_BASIC', 'E013', 5),
('PAPER_BASIC', 'E016', 6),
('PAPER_BASIC', 'E019', 7),
('PAPER_BASIC', 'E022', 8),
('PAPER_BASIC', 'E025', 9),
('PAPER_BASIC', 'E028', 10),
('PAPER_BASIC', 'E031', 11),
('PAPER_BASIC', 'E034', 12),
('PAPER_BASIC', 'E037', 13),
('PAPER_BASIC', 'E040', 14);
