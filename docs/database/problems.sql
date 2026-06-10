SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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

-- =========================
-- 增补：计算机组成原理（BJSL0001）题目
-- =========================
INSERT IGNORE INTO question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('Q_BJSL_201', '【知识点：指令集】在 RISC 处理器设计中，下面哪项通常是设计目标？', '简化指令集', 2, 2.5, 'BJSL0001', true),
('Q_BJSL_202', '【知识点：流水线】流水线中"冒险"通常分为哪三类？', '结构、数据、控制', 2, 3.0, 'BJSL0001', true),
('Q_BJSL_203', '【知识点：缓存】写回（write-back）缓存策略的特点是？', '写入只回写到主存在替换时', 2, 3.0, 'BJSL0001', true),
('Q_BJSL_204', '【知识点：虚拟存储】页表项中常包含的字段不包括下列哪一项？', '指令周期', 2, 2.5, 'BJSL0001', true),
('Q_BJSL_205', '【知识点：中断】中断服务例程（ISR）通常需要保存和恢复哪类信息？', '寄存器上下文', 2, 2.0, 'BJSL0001', true),
('Q_BJSL_206', '【知识点：流水线】为了传播数据前递（forwarding），硬件需要支持什么？', '从执行阶段向前级直接转发操作数', 2, 3.5, 'BJSL0001', true),
('Q_BJSL_207', '【知识点：总线】多主设备共享总线，常用的仲裁方式不包括？', '固定轮询外的随机仲裁', 2, 3.0, 'BJSL0001', true),
('Q_BJSL_208', '【知识点：时钟】提高时钟频率最直接带来的问题是？', '功耗和热设计增多', 2, 3.5, 'BJSL0001', true),
('Q_BJSL_209', '【知识点：存储】在层次化存储中，速度从快到慢的顺序一般是？', '寄存器→Cache→主存→外存', 2, 2.0, 'BJSL0001', true),
('Q_BJSL_210', '【知识点：寻址】相对寻址（relative addressing）通常基于哪个寄存器？', '程序计数器PC', 2, 2.5, 'BJSL0001', true),
('Q_BJSL_211', '【知识点：CPI】给定每条指令平均执行周期为 1.2，时钟周期时间 0.5ns，则 CPI × 时钟周期 = ?', '0.6ns', 3, 2.5, 'BJSL0001', true),
('Q_BJSL_212', '【知识点：分支预测】静态分支预测的简单策略之一是？', '总是预测不跳转', 2, 3.0, 'BJSL0001', true),
('Q_BJSL_213', '【知识点：微架构】流水线中插入气泡会带来什么后果？', '吞吐率下降', 2, 3.5, 'BJSL0001', true),
('Q_BJSL_214', '【知识点：译码】在经典五段流水线中，第几阶段负责指令译码？', 'ID 阶段', 2, 2.0, 'BJSL0001', true),
('Q_BJSL_215', '【知识点：ALU】ALU 运算通常不包括下列哪个操作？', '磁盘读写', 2, 1.5, 'BJSL0001', true),
('Q_BJSL_216', '【知识点：存储】在直接映射 Cache 中，给定地址与 Cache 行的映射是一一对应还是多对一？', '一一对应', 2, 2.5, 'BJSL0001', true),
('Q_BJSL_217', '【知识点：DMA】使用 DMA 的优点是？', '减轻 CPU 负载并提高传输效率', 2, 2.5, 'BJSL0001', true),
('Q_BJSL_218', '【知识点：控制】硬布线控制相比微指令控制的优点是？', '速度更快', 2, 3.0, 'BJSL0001', true),
('Q_BJSL_219', '【知识点：流水线】控制冒险通常由哪个指令行为导致？', '分支指令', 2, 2.5, 'BJSL0001', true),
('Q_BJSL_220', '【知识点：性能】Amdahl 定律关心的核心是什么？', '加速比受限于可并行部分比例', 3, 4.0, 'BJSL0001', true);

-- options for the above multiple-choice questions
INSERT IGNORE INTO question_option (question_id, option_text, option_order) VALUES
('Q_BJSL_201', '简化指令集', 1), ('Q_BJSL_201', '复杂指令集', 2), ('Q_BJSL_201', '更长的微指令', 3), ('Q_BJSL_201', '硬件虚拟化', 4),
('Q_BJSL_202', '结构、数据、控制', 1), ('Q_BJSL_202', '编译、链接、加载', 2), ('Q_BJSL_202', '寄存器、缓存、总线', 3), ('Q_BJSL_202', '分支、回滚、提交', 4),
('Q_BJSL_203', '写入只回写到主存在替换时', 1), ('Q_BJSL_203', '每次写都立即写主存', 2), ('Q_BJSL_203', '不写入主存', 3), ('Q_BJSL_203', '只读缓存', 4),
('Q_BJSL_204', '指令周期', 1), ('Q_BJSL_204', '物理帧号', 2), ('Q_BJSL_204', '有效位', 3), ('Q_BJSL_204', '访问权限', 4),
('Q_BJSL_205', '寄存器上下文', 1), ('Q_BJSL_205', '缓存行状态', 2), ('Q_BJSL_205', '硬盘布局', 3), ('Q_BJSL_205', 'GPIO 配置', 4),
('Q_BJSL_206', '从执行阶段向前级直接转发操作数', 1), ('Q_BJSL_206', '增加编译时间', 2), ('Q_BJSL_206', '减少寄存器数量', 3), ('Q_BJSL_206', '关闭缓存', 4),
('Q_BJSL_207', '固定轮询外的随机仲裁', 1), ('Q_BJSL_207', '集中式仲裁', 2), ('Q_BJSL_207', '分布式仲裁', 3), ('Q_BJSL_207', '轮询仲裁', 4),
('Q_BJSL_208', '功耗和热设计增多', 1), ('Q_BJSL_208', '增加内存容量', 2), ('Q_BJSL_208', '降低时钟周期', 3), ('Q_BJSL_208', '消除数据相关', 4),
('Q_BJSL_209', '寄存器→Cache→主存→外存', 1), ('Q_BJSL_209', '外存→主存→Cache→寄存器', 2), ('Q_BJSL_209', 'Cache→寄存器→外存→主存', 3), ('Q_BJSL_209', '主存→寄存器→Cache→外存', 4),
('Q_BJSL_210', '程序计数器PC', 1), ('Q_BJSL_210', '堆栈指针SP', 2), ('Q_BJSL_210', '状态寄存器SR', 3), ('Q_BJSL_210', '基址寄存器BR', 4),
('Q_BJSL_211', '0.6ns', 1), ('Q_BJSL_211', '0.06ns', 2), ('Q_BJSL_211', '6ns', 3), ('Q_BJSL_211', '0.5ns', 4),
('Q_BJSL_212', '总是预测不跳转', 1), ('Q_BJSL_212', '总是预测跳转', 2), ('Q_BJSL_212', '动态双向预测', 3), ('Q_BJSL_212', '延迟分支', 4),
('Q_BJSL_213', '吞吐率下降', 1), ('Q_BJSL_213', '功耗增加', 2), ('Q_BJSL_213', '缓存失效', 3), ('Q_BJSL_213', '指令丢失', 4),
('Q_BJSL_214', 'ID 阶段', 1), ('Q_BJSL_214', 'IF 阶段', 2), ('Q_BJSL_214', 'EX 阶段', 3), ('Q_BJSL_214', 'WB 阶段', 4),
('Q_BJSL_215', '磁盘读写', 1), ('Q_BJSL_215', '加法', 2), ('Q_BJSL_215', '移位', 3), ('Q_BJSL_215', '逻辑与', 4),
('Q_BJSL_216', '一一对应', 1), ('Q_BJSL_216', '多对一', 2), ('Q_BJSL_216', '一对多', 3), ('Q_BJSL_216', '无映射', 4),
('Q_BJSL_217', '减轻 CPU 负载并提高传输效率', 1), ('Q_BJSL_217', '增加中断数', 2), ('Q_BJSL_217', '降低内存容量', 3), ('Q_BJSL_217', '禁止缓存', 4),
('Q_BJSL_218', '速度更快', 1), ('Q_BJSL_218', '更易编程', 2), ('Q_BJSL_218', '占用更少晶体管', 3), ('Q_BJSL_218', '更低功耗', 4),
('Q_BJSL_219', '分支指令', 1), ('Q_BJSL_219', '算术指令', 2), ('Q_BJSL_219', '存储指令', 3), ('Q_BJSL_219', '输入输出指令', 4),
('Q_BJSL_220', '加速比受限于可并行部分比例', 1), ('Q_BJSL_220', '只考虑串行部分', 2), ('Q_BJSL_220', '与缓存无关', 3), ('Q_BJSL_220', '关注平均延迟', 4);

-- Additional blank (fill-in) questions for BJSL0001
INSERT IGNORE INTO blank_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('F_BJSL_001', '【知识点：CPI】若指令 A 的 CPI=1.0，占比 40%，指令 B 的 CPI=2.0，占比 60%，总体 CPI = ____。', '1.6', 3, 2.5, 'BJSL0001', true),
('F_BJSL_002', '【知识点：Cache】直接映射 Cache 中，给定地址映射到的行号为 ____（举例）', 'index bits', 3, 3.0, 'BJSL0001', true),
('F_BJSL_003', '【知识点：流水线】数据冒险通过 ____ 可部分缓解。', 'forwarding', 3, 2.5, 'BJSL0001', true),
('F_BJSL_004', '【知识点：寻址】基址加变址寻址中常用的两个寄存器是 ____ 和 ____ 。', 'base,index', 3, 3.5, 'BJSL0001', true),
('F_BJSL_005', '【知识点：中断】中断向量表通常存放在内存的 ____ 区域。', '低地址', 3, 2.0, 'BJSL0001', true),
('F_BJSL_006', '【知识点：DMA】使用 DMA 传输，可减少 CPU 的 ____。', '参与度', 3, 2.5, 'BJSL0001', true),
('F_BJSL_007', '【知识点：Cache】写直达（write-through）会降低写入延迟但增加 ____。', '内存带宽', 3, 3.0, 'BJSL0001', true),
('F_BJSL_008', '【知识点：流水线】延迟槽（delay slot）主要用于隐藏 ____ 的影响。', '分支惩罚', 3, 3.5, 'BJSL0001', true),
('F_BJSL_009', '【知识点：寄存器】通用寄存器用于保存操作数和 ____。', '中间结果', 3, 2.0, 'BJSL0001', true),
('F_BJSL_010', '【知识点：流水线】为减少控制冒险，常见硬件方法是使用 ____ 预测器。', '分支', 3, 3.0, 'BJSL0001', true);

-- Additional true/false (judge) questions for BJSL0001
INSERT IGNORE INTO judge_question_bank (id, stem, answer_key, points, difficulty, cno, active) VALUES
('J_BJSL_001', '【知识点：Cache】完全相联（fully associative）Cache 中每一块可以映射到任意行。', true, 2, 2.5, 'BJSL0001', true),
('J_BJSL_002', '【知识点：流水线】若所有指令的执行时间相同，流水线就可以使吞吐率线性增长。', true, 2, 3.0, 'BJSL0001', true),
('J_BJSL_003', '【知识点：DMA】DMA 传输不会产生中断。', false, 2, 2.0, 'BJSL0001', true),
('J_BJSL_004', '【知识点：时钟】提高频率总能线性提高单线程性能。', false, 2, 3.5, 'BJSL0001', true),
('J_BJSL_005', '【知识点：指令集】CISC 指令集倾向于使用复杂微代码实现复杂指令。', true, 2, 2.5, 'BJSL0001', true),
('J_BJSL_006', '【知识点：寻址】相对寻址依赖于当前 PC 值来计算目标地址。', true, 2, 2.0, 'BJSL0001', true);