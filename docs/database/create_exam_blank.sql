-- Requires course table from create.sql
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS blank_question_bank (
    id varchar(32) NOT NULL,
    stem varchar(512) NOT NULL,
    answer_key varchar(255) NOT NULL,
    points int NOT NULL,
    difficulty decimal(3,1) NOT NULL,
    difficulty_b decimal(4,2) NOT NULL DEFAULT 0.00,
    discrimination_a decimal(4,2) NOT NULL DEFAULT 1.00,
    cno char(8),
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    FOREIGN KEY (cno) REFERENCES course(cno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS blank_exam_paper (
    id varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS blank_exam_definition (
    id varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    description varchar(255),
    max_questions int NOT NULL,
    active boolean NOT NULL DEFAULT true,
    is_default boolean NOT NULL DEFAULT false,
    paper_id varchar(32) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (paper_id) REFERENCES blank_exam_paper(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS blank_exam_paper_question (
    paper_id varchar(32) NOT NULL,
    question_id varchar(32) NOT NULL,
    display_order int NOT NULL,
    PRIMARY KEY (paper_id, question_id),
    FOREIGN KEY (paper_id) REFERENCES blank_exam_paper(id),
    FOREIGN KEY (question_id) REFERENCES blank_question_bank(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO blank_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('F001', '【知识点：存储层次】速度最快但容量最小的存储层次是 ____ 。', '寄存器', 3, 1.5, 'BJSL0001', true),
('F002', '【知识点：指令周期】取指后通常进入 ____ 阶段。', '分析', 4, 3.0, 'BJSL0001', true),
('F003', '【知识点：总线】决定多个主设备访问次序的机制称为 ____ 。', '总线仲裁', 5, 4.5, 'BJSL0001', true),
('F004', '【知识点：存储结构】后进先出的线性结构称为 ____ 。', '栈', 3, 1.5, 'BJSL0002', true),
('F005', '【知识点：图遍历】无权图求最短路径常用 ____ 搜索。', '广度优先', 4, 3.0, 'BJSL0002', true),
('F006', '【知识点：排序】稳定排序中，等值元素的 ____ 保持不变。', '相对次序', 5, 4.5, 'BJSL0002', true),
('F007', '【知识点：进程】正在执行的程序实例称为 ____ 。', '进程', 3, 1.5, 'BJSL0003', true),
('F008', '【知识点：同步】互斥访问常用 ____ 量实现。', '信号', 4, 3.0, 'BJSL0003', true),
('F009', '【知识点：内存】将逻辑地址转换为物理地址的硬件称为 ____ 。', 'MMU', 5, 4.5, 'BJSL0003', true),
('F010', '【知识点：分层】OSI 模型的第 4 层是 ____ 层。', '传输', 3, 1.5, 'BJSL0004', true),
('F011', '【知识点：地址】IP 地址由 ____ 部分和主机部分组成。', '网络', 4, 3.0, 'BJSL0004', true),
('F012', '【知识点：可靠传输】TCP 使用 ____ 机制进行重传控制。', '确认', 5, 4.5, 'BJSL0004', true),
('F013', '【知识点：导数】d/dx x^2 的结果是 ____ 。', '2x', 3, 1.5, 'B07M0001', true),
('F014', '【知识点：积分】0 到 1 的 x dx 定积分结果是 ____ 。', '1/2', 4, 3.0, 'B07M0001', true),
('F015', '【知识点：极限】当 x -> 0 时，sin x / x 的极限为 ____ 。', '1', 5, 4.5, 'B07M0001', true),
('F016', '【知识点：矩阵】单位矩阵通常记为 ____ 。', 'I', 3, 1.5, 'B07M0002', true),
('F017', '【知识点：行列式】2x2 行列式为 0 表示矩阵 ____ 。', '不可逆', 4, 3.0, 'B07M0002', true),
('F018', '【知识点：向量】向量组线性相关意味着存在非零系数使其 ____ 。', '线性组合为零', 5, 4.5, 'B07M0002', true),
('F019', '【知识点：SQL】用于筛选行的子句是 ____ 。', 'WHERE', 3, 1.5, 'B09D0001', true),
('F020', '【知识点：范式】消除部分依赖是 ____ 范式的要求。', '2NF', 4, 3.0, 'B09D0001', true),
('F021', '【知识点：索引】B+ 树叶子结点通过 ____ 指针相连。', '链表', 5, 4.5, 'B09D0001', true),
('F022', '【知识点：文法】文法的开始符号称为 ____ 。', '开始符号', 3, 1.5, 'B71C0001', true),
('F023', '【知识点：词法】识别单词常用 ____ 自动机。', '有限', 4, 3.0, 'B71C0001', true),
('F024', '【知识点：语法分析】自顶向下分析构造 ____ 树。', '语法', 5, 4.5, 'B71C0001', true),
('F025', '【知识点：需求】需求规格说明书简称 ____ 。', 'SRS', 3, 1.5, 'B71S0001', true),
('F026', '【知识点：测试】黑盒测试也称 ____ 测试。', '功能', 4, 3.0, 'B71S0001', true),
('F027', '【知识点：过程】瀑布模型强调阶段 ____ 。', '顺序', 5, 4.5, 'B71S0001', true),
('F028', '【知识点：指针】空指针常用关键字是 ____ 。', 'nullptr', 3, 1.5, 'BJSL0012', true),
('F029', '【知识点：引用】引用的底层实现通常等价于 ____ 。', '指针', 4, 3.0, 'BJSL0012', true),
('F030', '【知识点：对象初始化】类中用于初始化对象的函数是 ____ 。', '构造函数', 5, 4.5, 'BJSL0012', true),
('F031', '【知识点：运动学】匀速直线运动的速度大小保持 ____ 。', '不变', 3, 1.5, 'B10M0001', true),
('F032', '【知识点：能量】动能公式为 ____ 。', '1/2 m v^2', 4, 3.0, 'B10M0001', true),
('F033', '【知识点：电学】欧姆定律表示为 ____ 。', 'U=IR', 5, 4.5, 'B10M0001', true),
('F034', '【知识点：监督学习】有标签的数据训练称为 ____ 学习。', '监督', 3, 1.5, 'B58I0001', true),
('F035', '【知识点：分类】二分类常用的激活函数之一是 ____ 。', 'sigmoid', 4, 3.0, 'B58I0001', true),
('F036', '【知识点：评估】分类准确率可表示为正确数/ ____ 。', '总数', 5, 4.5, 'B58I0001', true),
('F037', '【知识点：复杂度】二分查找的时间复杂度为 ____ 。', 'O(log n)', 3, 1.5, 'B09A0001', true),
('F038', '【知识点：图算法】最小生成树常用算法有 Prim 和 ____ 。', 'Kruskal', 4, 3.0, 'B09A0001', true),
('F039', '【知识点：分治】分治法的核心步骤是分解、解决和 ____ 。', '合并', 5, 4.5, 'B09A0001', true),
('F040', '【知识点：语法】句子的主谓结构中，谓语通常是 ____ 。', '动词', 3, 1.5, 'B17M0001', true),
('F041', '【知识点：写作】段落中的过渡词用于表达 ____ 关系。', '逻辑', 4, 3.0, 'B17M0001', true),
('F042', '【知识点：阅读】快速获取主旨的阅读方法称为 ____ 。', '略读', 5, 4.5, 'B17M0001', true);

INSERT IGNORE INTO blank_exam_paper (id, name, active) VALUES
('PAPER_DEFAULT', '填空题题库', true),
('PAPER_BASIC', '填空题基础题库', true);

INSERT IGNORE INTO blank_exam_definition (id, name, description, max_questions, active, is_default, paper_id) VALUES
('EXAM_DEFAULT', '填空题标准考试', '覆盖全部填空题', 42, true, true, 'PAPER_DEFAULT'),
('EXAM_BASIC', '填空题基础考试', '基础填空题集合', 14, true, false, 'PAPER_BASIC');

INSERT IGNORE INTO blank_exam_paper_question (paper_id, question_id, display_order) VALUES
('PAPER_DEFAULT', 'F001', 1),
('PAPER_DEFAULT', 'F002', 2),
('PAPER_DEFAULT', 'F003', 3),
('PAPER_DEFAULT', 'F004', 4),
('PAPER_DEFAULT', 'F005', 5),
('PAPER_DEFAULT', 'F006', 6),
('PAPER_DEFAULT', 'F007', 7),
('PAPER_DEFAULT', 'F008', 8),
('PAPER_DEFAULT', 'F009', 9),
('PAPER_DEFAULT', 'F010', 10),
('PAPER_DEFAULT', 'F011', 11),
('PAPER_DEFAULT', 'F012', 12),
('PAPER_DEFAULT', 'F013', 13),
('PAPER_DEFAULT', 'F014', 14),
('PAPER_DEFAULT', 'F015', 15),
('PAPER_DEFAULT', 'F016', 16),
('PAPER_DEFAULT', 'F017', 17),
('PAPER_DEFAULT', 'F018', 18),
('PAPER_DEFAULT', 'F019', 19),
('PAPER_DEFAULT', 'F020', 20),
('PAPER_DEFAULT', 'F021', 21),
('PAPER_DEFAULT', 'F022', 22),
('PAPER_DEFAULT', 'F023', 23),
('PAPER_DEFAULT', 'F024', 24),
('PAPER_DEFAULT', 'F025', 25),
('PAPER_DEFAULT', 'F026', 26),
('PAPER_DEFAULT', 'F027', 27),
('PAPER_DEFAULT', 'F028', 28),
('PAPER_DEFAULT', 'F029', 29),
('PAPER_DEFAULT', 'F030', 30),
('PAPER_DEFAULT', 'F031', 31),
('PAPER_DEFAULT', 'F032', 32),
('PAPER_DEFAULT', 'F033', 33),
('PAPER_DEFAULT', 'F034', 34),
('PAPER_DEFAULT', 'F035', 35),
('PAPER_DEFAULT', 'F036', 36),
('PAPER_DEFAULT', 'F037', 37),
('PAPER_DEFAULT', 'F038', 38),
('PAPER_DEFAULT', 'F039', 39),
('PAPER_DEFAULT', 'F040', 40),
('PAPER_DEFAULT', 'F041', 41),
('PAPER_DEFAULT', 'F042', 42),
('PAPER_BASIC', 'F001', 1),
('PAPER_BASIC', 'F004', 2),
('PAPER_BASIC', 'F007', 3),
('PAPER_BASIC', 'F010', 4),
('PAPER_BASIC', 'F013', 5),
('PAPER_BASIC', 'F016', 6),
('PAPER_BASIC', 'F019', 7),
('PAPER_BASIC', 'F022', 8),
('PAPER_BASIC', 'F025', 9),
('PAPER_BASIC', 'F028', 10),
('PAPER_BASIC', 'F031', 11),
('PAPER_BASIC', 'F034', 12),
('PAPER_BASIC', 'F037', 13),
('PAPER_BASIC', 'F040', 14);
