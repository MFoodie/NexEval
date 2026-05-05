INSERT IGNORE INTO question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('Q_NEW_001', '【知识点：总线控制】在集中式总线仲裁中，对电路故障最敏感的方式是？', '链式查询', 2, 2.5, 'BJSL0001', true),
('Q_NEW_002', '【知识点：网络安全】防止重放攻击（Replay Attack）最常用的技术手段是？', '时间戳和随机数', 3, 3.5, 'BJSL0004', true),
('Q_NEW_003', '【知识点：聚类算法】DBSCAN 算法相比 K-Means 的显著优势是什么？', '能发现任意形状的簇且无需预设簇数K', 4, 4.0, 'B58I0001', true);

INSERT IGNORE INTO question_option (question_id, option_text, option_order) VALUES
('Q_NEW_001', '链式查询', 1), ('Q_NEW_001', '计数器定时查询', 2), ('Q_NEW_001', '独立请求', 3), ('Q_NEW_001', '分布式仲裁', 4),
('Q_NEW_002', '对称加密', 1), ('Q_NEW_002', '时间戳和随机数', 2), ('Q_NEW_002', '数字签名', 3), ('Q_NEW_002', '报文摘要', 4),
('Q_NEW_003', '计算复杂度极低', 1), ('Q_NEW_003', '对参数不敏感', 2), ('Q_NEW_003', '能发现任意形状的簇且无需预设簇数K', 3), ('Q_NEW_003', '完美处理高维稀疏数据', 4);

INSERT IGNORE INTO blank_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('F_NEW_001', '【知识点：等价无穷小】若 lim(x->0) (x-tan x)(e^x-x-1)/(1-cos x)(x-sin x)，其极限值为 ____。', '-2', 3, 2.0, 'B07M0001', true),
('F_NEW_002', '【知识点：智能指针】C++11 中，解决 shared_ptr 循环引用问题的指针类型是 ____。', 'weak_ptr', 4, 3.5, 'BJSL0012', true),
('F_NEW_003', '【知识点：级数求和】计算 Σ_{n=1}^∞ 1/n^2 值为 ____。', 'π^2/6', 3, 2.0, 'B07M0001', true),
('F_NEW_004', '【知识点：词法分析】正规表达式 (a|b)*abb 可以识别以 ____ 结尾的字符串。', 'abb', 3, 2.5, 'B71C0001', true);

INSERT IGNORE INTO essay_question_bank (id, stem, points, difficulty, cno, active) VALUES
('E_NEW_001', '【知识点：数据库并发】请简述封锁协议中的两段锁协议（2PL），并说明它与可串行化调度的关系。', 10, 4.0, 'B09D0001', true),
('E_NEW_002', '【知识点：软件设计】什么是依赖倒置原则（DIP）？请结合面向接口编程的思想举例说明。', 8, 3.5, 'B71S0001', true),
('E_NEW_003', '【知识点：贪心算法】证明霍夫曼编码（Huffman Coding）能够产生前缀码中的最优编码。', 12, 4.5, 'B09A0001', true);

INSERT IGNORE INTO judge_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('J_NEW_001', '【知识点：矩阵运算】任意两个 n 阶方阵 A 和 B，恒有 AB = BA 成立。', false, 2, 1.5, 'B07M0002', true),
('J_NEW_002', '【知识点：电磁学】静电场中，电场线总是从高电势指向低电势。', true, 2, 2.0, 'B10M0001', true),
('J_NEW_003', '【知识点：学术写作】In an academic essay, the thesis statement is usually found in the conclusion paragraph.', false, 2, 1.5, 'B17M0001', true);

INSERT IGNORE INTO question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('Q_EXT_001', '【知识点：B树】一棵高度为 h 的 m 阶 B 树，其根结点最多包含几个关键字？', 'm-1', 4, 3.5, 'BJSL0002', true),
('Q_EXT_002', '【知识点：图的连通性】n 个顶点的强连通有向图至少包含几条边？', 'n', 3, 3.0, 'BJSL0002', true),
('Q_EXT_003', '【知识点：最短路径】Floyd 算法的本质是哪种算法设计策略？', '动态规划', 5, 4.0, 'BJSL0002', true),
('Q_EXT_004', '【知识点：死锁预防】破坏“循环等待”条件通常采用的方法是？', '资源有序分配法', 4, 3.5, 'BJSL0003', true),
('Q_EXT_005', '【知识点：内存分配】在动态分区分配中，最容易产生外部碎片的算法是？', '最佳适应算法(Best Fit)', 5, 4.5, 'BJSL0003', true);

INSERT IGNORE INTO question_option (question_id, option_text, option_order) VALUES
('Q_EXT_001', 'm', 1), ('Q_EXT_001', 'm-1', 2), ('Q_EXT_001', 'm/2', 3), ('Q_EXT_001', '2m', 4),
('Q_EXT_002', 'n-1', 1), ('Q_EXT_002', 'n', 2), ('Q_EXT_002', 'n+1', 3), ('Q_EXT_002', 'n(n-1)/2', 4),
('Q_EXT_003', '贪心算法', 1), ('Q_EXT_003', '分治法', 2), ('Q_EXT_003', '动态规划', 3), ('Q_EXT_003', '回溯法', 4),
('Q_EXT_004', '资源有序分配法', 1), ('Q_EXT_004', '剥夺资源法', 2), ('Q_EXT_004', '静态分配法', 3), ('Q_EXT_004', '银行家算法', 4),
('Q_EXT_005', '首次适应算法(First Fit)', 1), ('Q_EXT_005', '最佳适应算法(Best Fit)', 2), ('Q_EXT_005', '最坏适应算法(Worst Fit)', 3), ('Q_EXT_005', '循环首次适应算法', 4);

INSERT IGNORE INTO blank_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('F_EXT_001', '【知识点：二叉树】已知一棵完全二叉树有 1001 个结点，则其叶子结点的个数为 ____。', '501', 5, 4.0, 'BJSL0002', true),
('F_EXT_002', '【知识点：排序】在快速排序的一趟划分中，作为划分标准的元素被称为 ____。', '枢轴（或基准）', 3, 2.0, 'BJSL0002', true),
('F_EXT_003', '【知识点：进程通信】管道（Pipe）通信只能实现 ____ 的数据传输。', '半双工', 3, 2.5, 'BJSL0003', true),
('F_EXT_004', '【知识点：设备管理】SPOOLing 技术的全称是外部设备联机 ____ 操作。', '并行', 4, 3.5, 'BJSL0003', true);

INSERT IGNORE INTO essay_question_bank (id, stem, points, difficulty, cno, active) VALUES
('E_EXT_001', '【知识点：平衡二叉树】请简述 AVL 树的四种旋转调整操作（LL, RR, LR, RL），并分别画出简图说明。', 12, 4.5, 'BJSL0002', true),
('E_EXT_002', '【知识点：哈希表】什么是装载因子（Load Factor）？它对哈希表的查找效率和空间利用率有什么影响？', 8, 3.0, 'BJSL0002', true),
('E_EXT_003', '【知识点：银行家算法】请说明银行家算法中 Available, Max, Allocation, Need 四个数据结构的含义及它们之间的等式关系。', 10, 4.0, 'BJSL0003', true),
('E_EXT_004', '【知识点：页面置换】比较 FIFO、LRU 和 OPT 三种页面置换算法的优缺点及在实际系统中的可行性。', 12, 4.5, 'BJSL0003', true);

INSERT IGNORE INTO judge_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('J_EXT_001', '【知识点：树】森林与二叉树可以相互转换，且转换后的二叉树根结点一定没有右子树。', true, 3, 3.5, 'BJSL0002', true),
('J_EXT_002', '【知识点：图】用邻接表存储的无向图，其空间复杂度为 O(V+E)。', true, 2, 2.5, 'BJSL0002', true),
('J_EXT_003', '【知识点：线程】在多线程模型中，同一个进程内的不同线程拥有各自独立的堆内存。', false, 4, 4.0, 'BJSL0003', true),
('J_EXT_004', '【知识点：文件系统】硬链接可以跨越不同的文件系统建立。', false, 3, 3.0, 'BJSL0003', true);